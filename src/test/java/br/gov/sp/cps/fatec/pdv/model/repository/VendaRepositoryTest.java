package br.gov.sp.cps.fatec.pdv.model.repository;

import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.model.entity.Venda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VendaRepositoryTest {

    private static EntityManagerFactory emf;
    private ProdutoRepository produtoRepository;
    private VendaRepository vendaRepository;

    @BeforeAll
    static void iniciarBancoDeTeste() {
        emf = Persistence.createEntityManagerFactory("pdv-test-pu");
    }

    @AfterAll
    static void encerrarBancoDeTeste() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    void prepararRepositorios() {
        produtoRepository = new ProdutoRepositoryImpl(emf);
        vendaRepository = new VendaRepositoryImpl(emf);
    }

    @Test
    @DisplayName("Deve registrar venda, totalizar itens e baixar estoque atomicamente")
    void deveRegistrarVendaComItensECliente() {
        Produto produto = salvarProduto("SALE-VALID-1", "Produto venda", "12.50", 10);

        Venda venda = vendaRepository.registrar(
                " Cliente de teste ",
                List.of(new ItemVendaSolicitado(produto.getCodigoBarras(), 2)));

        assertNotNull(venda.getId());
        assertEquals(new BigDecimal("25.00"), venda.getValorTotal());
        assertEquals("Cliente de teste", venda.getCliente().getNome());
        assertEquals(1, venda.getItens().size());
        assertEquals("Produto venda", venda.getItens().get(0).getNomeProduto());
        assertEquals(new BigDecimal("12.50"), venda.getItens().get(0).getPrecoUnitario());
        assertEquals(8, produtoRepository.buscarPorId(produto.getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    @DisplayName("Deve consolidar itens repetidos do mesmo produto")
    void deveConsolidarItensRepetidos() {
        Produto produto = salvarProduto("SALE-DUP-2", "Produto repetido", "3.25", 10);

        Venda venda = vendaRepository.registrar(
                null,
                List.of(
                        new ItemVendaSolicitado(produto.getCodigoBarras(), 2),
                        new ItemVendaSolicitado(produto.getCodigoBarras(), 3)));

        assertEquals(1, venda.getItens().size());
        assertEquals(5, venda.getItens().get(0).getQuantidade());
        assertEquals(new BigDecimal("16.25"), venda.getValorTotal());
        assertNull(venda.getCliente());
        assertEquals(5, produtoRepository.buscarPorId(produto.getId()).orElseThrow().getQuantidadeEstoque());
    }

    @Test
    @DisplayName("Deve reverter venda, cliente e estoque se algum item falhar")
    void deveReverterTodaVendaQuandoItemFalha() {
        Produto disponivel = salvarProduto("SALE-ROLLBACK-3", "Produto disponível", "2.00", 10);
        Produto semEstoque = salvarProduto("SALE-ROLLBACK-4", "Produto sem estoque", "5.00", 1);
        long vendasAntes = contar("SELECT COUNT(v) FROM Venda v");
        long clientesAntes = contar("SELECT COUNT(c) FROM Cliente c");

        assertThrows(IllegalArgumentException.class, () -> vendaRepository.registrar(
                "Cliente deve sofrer rollback",
                List.of(
                        new ItemVendaSolicitado(disponivel.getCodigoBarras(), 4),
                        new ItemVendaSolicitado(semEstoque.getCodigoBarras(), 2))));

        assertEquals(10, produtoRepository.buscarPorId(disponivel.getId()).orElseThrow().getQuantidadeEstoque());
        assertEquals(1, produtoRepository.buscarPorId(semEstoque.getId()).orElseThrow().getQuantidadeEstoque());
        assertEquals(vendasAntes, contar("SELECT COUNT(v) FROM Venda v"));
        assertEquals(clientesAntes, contar("SELECT COUNT(c) FROM Cliente c"));
    }

    @Test
    @DisplayName("Deve rejeitar lista vazia e produto inexistente")
    void deveRejeitarVendaSemItensOuComProdutoInexistente() {
        assertThrows(IllegalArgumentException.class, () -> vendaRepository.registrar("Cliente", List.of()));
        assertThrows(IllegalArgumentException.class, () -> vendaRepository.registrar(
                null,
                List.of(new ItemVendaSolicitado("CODIGO-INEXISTENTE", 1))));
    }

    @Test
    @DisplayName("Deve listar vendas com cliente e itens acessíveis após fechar a sessão JPA")
    void deveConsultarHistoricoComRelacionamentosCarregados() {
        Produto produto = salvarProduto("SALE-HISTORY-5", "Produto histórico", "7.25", 4);
        vendaRepository.registrar(
                "Cliente histórico",
                List.of(new ItemVendaSolicitado(produto.getCodigoBarras(), 2)));

        List<Venda> vendas = vendaRepository.listarTodas();
        Venda venda = vendas.stream()
                .filter(item -> item.getCliente() != null
                        && "Cliente histórico".equals(item.getCliente().getNome()))
                .findFirst()
                .orElseThrow();

        assertEquals(1, venda.getItens().size());
        assertEquals("Produto histórico", venda.getItens().get(0).getNomeProduto());
        assertEquals("SALE-HISTORY-5", venda.getItens().get(0).getProduto().getCodigoBarras());
        assertEquals(new BigDecimal("14.50"), venda.getValorTotal());
    }

    private Produto salvarProduto(String codigo, String nome, String preco, int estoque) {
        return produtoRepository.salvar(new Produto(codigo, nome, new BigDecimal(preco), estoque));
    }

    private long contar(String jpql) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(jpql, Long.class).getSingleResult();
        } finally {
            em.close();
        }
    }
}
