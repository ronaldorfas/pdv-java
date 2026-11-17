package br.gov.sp.cps.fatec.pdv.model.service;

import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.model.observer.ProdutoObserver;
import br.gov.sp.cps.fatec.pdv.model.repository.ProdutoRepository;
import br.gov.sp.cps.fatec.pdv.model.repository.ProdutoRepositoryImpl;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes Unitários das Regras de Negócio da Camada de Serviço e do Padrão Observer.
 */
class ProdutoServiceTest {

    private static EntityManagerFactory emf;
    private ProdutoRepository repository;
    private ProdutoService service;

    @BeforeAll
    static void setUpAll() {
        emf = Persistence.createEntityManagerFactory("pdv-test-pu");
    }

    @AfterAll
    static void tearDownAll() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    void setUp() {
        repository = new ProdutoRepositoryImpl(emf);
        service = new ProdutoService(repository);
    }

    @Test
    @DisplayName("Deve rejeitar produto com preço negativo ou zero")
    void deveRejeitarPrecoInvalido() {
        Produto pZero = new Produto("111", "Item Zero", BigDecimal.ZERO, 10);
        assertThrows(IllegalArgumentException.class, () -> service.salvar(pZero));

        Produto pNegativo = new Produto("112", "Item Negativo", new BigDecimal("-5.00"), 10);
        assertThrows(IllegalArgumentException.class, () -> service.salvar(pNegativo));
    }

    @Test
    @DisplayName("Deve rejeitar preço acima da precisão ou escala da coluna")
    void deveRejeitarPrecoForaDaPrecisaoDoBanco() {
        Produto escalaExcedida = new Produto("114", "Preço com escala excedida", new BigDecimal("1.234"), 1);
        IllegalArgumentException erroEscala = assertThrows(
                IllegalArgumentException.class,
                () -> service.salvar(escalaExcedida));
        assertTrue(erroEscala.getMessage().contains("duas casas decimais"));

        Produto valorExcedido = new Produto(
                "115",
                "Preço acima do limite",
                new BigDecimal("10000000000.00"),
                1);
        IllegalArgumentException erroValor = assertThrows(
                IllegalArgumentException.class,
                () -> service.salvar(valorExcedido));
        assertTrue(erroValor.getMessage().contains("9.999.999.999,99"));
    }

    @Test
    @DisplayName("Deve rejeitar código de barras e nome acima dos limites do banco")
    void deveRejeitarCamposAcimaDoLimite() {
        Produto codigoLongo = new Produto("1".repeat(51), "Produto válido", new BigDecimal("1.00"), 0);
        IllegalArgumentException erroCodigo = assertThrows(
                IllegalArgumentException.class,
                () -> service.salvar(codigoLongo));
        assertTrue(erroCodigo.getMessage().contains("50 caracteres"));

        Produto nomeLongo = new Produto("116", "N".repeat(121), new BigDecimal("1.00"), 0);
        IllegalArgumentException erroNome = assertThrows(
                IllegalArgumentException.class,
                () -> service.salvar(nomeLongo));
        assertTrue(erroNome.getMessage().contains("120 caracteres"));
    }

    @Test
    @DisplayName("Deve remover espaços externos de código e nome antes de persistir")
    void deveNormalizarCamposTextuais() {
        Produto produto = new Produto(" 117 ", " Produto com espaços ", new BigDecimal("1.00"), 0);
        service.salvar(produto);

        assertEquals("117", produto.getCodigoBarras());
        assertEquals("Produto com espaços", produto.getNome());
    }

    @Test
    @DisplayName("Deve rejeitar produto com quantidade em estoque negativa")
    void deveRejeitarEstoqueNegativo() {
        Produto p = new Produto("113", "Item Estoque Inválido", new BigDecimal("10.00"), -1);
        assertThrows(IllegalArgumentException.class, () -> service.salvar(p));
    }

    @Test
    @DisplayName("Deve rejeitar produto com código de barras duplicado")
    void deveRejeitarCodigoBarrasDuplicado() {
        Produto p1 = new Produto("9999", "Produto Original", new BigDecimal("15.00"), 5);
        service.salvar(p1);

        Produto p2 = new Produto("9999", "Produto Clonado", new BigDecimal("20.00"), 8);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.salvar(p2));
        assertTrue(ex.getMessage().contains("Já existe um produto cadastrado com o código de barras"));
    }

    @Test
    @DisplayName("Deve atualizar produto e permitir manter seu próprio código")
    void deveAtualizarProdutoMantendoCodigoProprio() {
        Produto salvo = service.salvar(
                new Produto("700001", "Produto antes da edição", new BigDecimal("10.00"), 5));

        Produto atualizado = service.atualizar(
                new Produto(salvo.getId(), "700001", "Produto atualizado", new BigDecimal("12.50"), 8));

        assertEquals(salvo.getId(), atualizado.getId());
        assertEquals("Produto atualizado", atualizado.getNome());
        assertEquals(new BigDecimal("12.50"), atualizado.getPreco());
        assertEquals(8, atualizado.getQuantidadeEstoque());
    }

    @Test
    @DisplayName("Deve rejeitar atualização com código pertencente a outro produto")
    void deveRejeitarCodigoDuplicadoNaAtualizacao() {
        Produto primeiro = service.salvar(
                new Produto("700002", "Primeiro produto", new BigDecimal("10.00"), 5));
        Produto segundo = service.salvar(
                new Produto("700003", "Segundo produto", new BigDecimal("11.00"), 6));

        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.atualizar(new Produto(
                        segundo.getId(),
                        primeiro.getCodigoBarras(),
                        "Tentativa duplicada",
                        new BigDecimal("12.00"),
                        7)));

        assertTrue(erro.getMessage().contains("já pertence a outro produto"));
        assertEquals("Segundo produto", repository.buscarPorId(segundo.getId()).orElseThrow().getNome());
    }

    @Test
    @DisplayName("Deve rejeitar atualização de produto nulo ou inexistente")
    void deveRejeitarAtualizacaoInvalida() {
        assertThrows(IllegalArgumentException.class, () -> service.atualizar(null));

        Produto inexistente = new Produto(999999L, "700004", "Não cadastrado", new BigDecimal("1.00"), 0);
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.atualizar(inexistente));
        assertTrue(erro.getMessage().contains("Produto não encontrado"));
    }

    @Test
    @DisplayName("Deve rejeitar exclusão de produto inexistente")
    void deveRejeitarExclusaoInexistente() {
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> service.excluir(999998L));
        assertTrue(erro.getMessage().contains("Produto não encontrado"));
    }

    @Test
    @DisplayName("Deve cadastrar produto válido e notificar observador")
    void deveSalvarENotificarObservadores() throws InterruptedException {
        CountDownLatch notificacaoRecebida = new CountDownLatch(1);

        ProdutoObserver observer = produtos -> {
            notificacaoRecebida.countDown();
        };
        service.addObserver(observer);

        Produto p = new Produto("8888", "Produto Observer Test", new BigDecimal("12.50"), 10);
        service.salvar(p);

        assertTrue(
                notificacaoRecebida.await(2, TimeUnit.SECONDS),
                "O observador deve ser notificado após salvar o produto");
    }
}
