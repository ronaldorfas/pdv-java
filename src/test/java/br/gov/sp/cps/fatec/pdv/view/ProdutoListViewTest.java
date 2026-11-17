package br.gov.sp.cps.fatec.pdv.view;

import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProdutoListViewTest {

    @Test
    @DisplayName("Deve filtrar por nome e código sem perder a referência correta do produto")
    void deveFiltrarESelecionarProdutoCorreto() throws Exception {
        AtomicReference<Throwable> falha = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            try {
                ProdutoListView view = new ProdutoListView();
                Produto banana = new Produto(1L, "789-BAN", "Banana", new BigDecimal("2.50"), 12);
                Produto arroz = new Produto(2L, "789-ARR", "Arroz", new BigDecimal("8.90"), 20);
                view.atualizarTabela(List.of(banana, arroz));

                view.getTabelaProdutos().getRowSorter().toggleSortOrder(2);
                view.getTabelaProdutos().setRowSelectionInterval(0, 0);
                assertEquals(arroz, view.getProdutoSelecionado());

                view.getCampoBusca().setText("banana");
                assertEquals(1, view.getTabelaProdutos().getRowCount());
                assertNull(view.getProdutoSelecionado());
                view.getTabelaProdutos().setRowSelectionInterval(0, 0);
                assertEquals(banana, view.getProdutoSelecionado());

                view.getCampoBusca().setText("789-ARR");
                assertEquals(1, view.getTabelaProdutos().getRowCount());
                view.getTabelaProdutos().setRowSelectionInterval(0, 0);
                assertEquals(arroz, view.getProdutoSelecionado());

                view.getBtnLimparBusca().doClick();
                assertEquals(2, view.getTabelaProdutos().getRowCount());

                view.getCampoBusca().setText("não existe");
                assertEquals(0, view.getTabelaProdutos().getRowCount());
                assertNull(view.getProdutoSelecionado());
            } catch (Throwable e) {
                falha.set(e);
            }
        });

        if (falha.get() != null) {
            throw new AssertionError("Falha ao validar a listagem de produtos.", falha.get());
        }
    }
}
