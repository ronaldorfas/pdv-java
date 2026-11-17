package br.gov.sp.cps.fatec.pdv.view;

import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProdutoFormViewTest {

    @Test
    @DisplayName("Deve carregar, limpar e retornar ao modo de cadastro corretamente")
    void deveAlternarEntreCadastroEdicaoELimpeza() throws Exception {
        executarNaEdt(form -> {
            Produto produto = new Produto(42L, "78942", "Produto de teste", new BigDecimal("19.99"), 15);

            form.carregarParaEdicao(produto);
            assertEquals(42L, form.getProdutoIdAtual());
            assertEquals("78942", form.getCodigoBarras());
            assertEquals("Produto de teste", form.getNome());
            assertEquals("19.99", form.getPrecoTexto());
            assertEquals("15", form.getQuantidadeEstoqueTexto());
            assertEquals("Salvar Alterações", form.getBtnSalvar().getText());

            form.limparCampos();
            assertEquals(42L, form.getProdutoIdAtual());
            assertEquals("", form.getCodigoBarras());
            assertEquals("", form.getNome());
            assertEquals("", form.getPrecoTexto());
            assertEquals("", form.getQuantidadeEstoqueTexto());

            form.configurarModoNovo();
            assertNull(form.getProdutoIdAtual());
            assertEquals("Cadastrar", form.getBtnSalvar().getText());
        });
    }

    @Test
    @DisplayName("Deve restringir o comprimento dos campos textuais e numéricos")
    void deveLimitarComprimentoDosCampos() throws Exception {
        executarNaEdt(form -> {
            List<JTextField> campos = new ArrayList<>();
            coletarCampos(form, campos);
            assertEquals(5, campos.size());

            campos.get(1).setText("C".repeat(51));
            campos.get(2).setText("N".repeat(121));
            campos.get(3).setText("1".repeat(14));
            campos.get(4).setText("1".repeat(11));

            assertEquals(50, form.getCodigoBarras().length());
            assertEquals(120, form.getNome().length());
            assertEquals(13, form.getPrecoTexto().length());
            assertEquals(10, form.getQuantidadeEstoqueTexto().length());
        });
    }

    private static void coletarCampos(Container container, List<JTextField> campos) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTextField campo) {
                campos.add(campo);
            } else if (component instanceof Container filho) {
                coletarCampos(filho, campos);
            }
        }
    }

    private static void executarNaEdt(java.util.function.Consumer<ProdutoFormView> teste) throws Exception {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                teste.accept(new ProdutoFormView());
            } catch (Throwable e) {
                falha.set(e);
            }
        });
        if (falha.get() != null) {
            throw new AssertionError("Falha ao validar o formulário de produtos.", falha.get());
        }
    }
}
