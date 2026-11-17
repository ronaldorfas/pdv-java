package br.gov.sp.cps.fatec.pdv.view;

import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.model.repository.ItemVendaSolicitado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendaViewTest {

    @Test
    @DisplayName("Deve manter itens do carrinho sincronizados ao remover e limpar")
    void deveSincronizarItensDoCarrinho() throws Exception {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                VendaView view = new VendaView();
                Produto cafe = new Produto(1L, "789-1", "Café", new BigDecimal("12.50"), 8);
                Produto arroz = new Produto(2L, "789-2", "Arroz", new BigDecimal("9.00"), 4);
                view.adicionarItem(new ItemVendaSolicitado("789-1", 2), cafe);
                view.adicionarItem(new ItemVendaSolicitado("789-2", 1), arroz);

                assertEquals(2, view.getItensSolicitados().size());
                assertEquals(2, view.getItensSolicitados().get(0).quantidade());
                assertEquals(0, new BigDecimal("34.00").compareTo(view.getValorParcial()));
                assertTrue(view.getLblValorParcial().getText().contains("34,00"));

                view.getTabelaItens().setRowSelectionInterval(0, 0);
                assertTrue(view.removerItemSelecionado());
                assertEquals(1, view.getItensSolicitados().size());
                assertEquals("789-2", view.getItensSolicitados().get(0).codigoBarras());
                assertEquals(0, new BigDecimal("9.00").compareTo(view.getValorParcial()));

                view.limparVenda();
                assertEquals(0, view.getItensSolicitados().size());
                assertEquals(0, BigDecimal.ZERO.compareTo(view.getValorParcial()));
                assertTrue(view.getBtnFinalizar().isEnabled());
            } catch (Throwable e) {
                falha.set(e);
            }
        });
        if (falha.get() != null) {
            throw new AssertionError("Falha ao validar a tela de venda.", falha.get());
        }
    }

    @Test
    @DisplayName("Deve filtrar os produtos da lista pelo nome")
    void deveFiltrarProdutosPeloNome() throws Exception {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                VendaView view = new VendaView();
                Produto cafe = new Produto(1L, "789-1", "Café especial", new BigDecimal("12.50"), 8);
                Produto arroz = new Produto(2L, "789-2", "Arroz integral", new BigDecimal("9.00"), 4);
                view.atualizarProdutos(List.of(cafe, arroz));

                view.getCampoBuscaProduto().setText("CAFÉ");

                assertEquals(1, view.getListaProdutos().getModel().getSize());
                assertSame(cafe, view.getListaProdutos().getModel().getElementAt(0));
                view.getListaProdutos().setSelectedIndex(0);
                assertSame(cafe, view.getProdutoSelecionado());
            } catch (Throwable e) {
                falha.set(e);
            }
        });
        if (falha.get() != null) {
            throw new AssertionError("Falha ao validar a busca de produtos na tela de venda.", falha.get());
        }
    }
}
