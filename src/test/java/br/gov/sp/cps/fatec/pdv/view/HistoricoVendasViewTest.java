package br.gov.sp.cps.fatec.pdv.view;

import br.gov.sp.cps.fatec.pdv.model.entity.Cliente;
import br.gov.sp.cps.fatec.pdv.model.entity.ItemVenda;
import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.model.entity.Venda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HistoricoVendasViewTest {

    @Test
    @DisplayName("Deve mostrar resumo e itens da venda selecionada")
    void deveMostrarResumoEDetalhesDaVenda() throws Exception {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                HistoricoVendasView view = new HistoricoVendasView();
                Produto produto = new Produto(8L, "HISTORY-8", "Café", new BigDecimal("4.50"), 2);
                Venda venda = new Venda(LocalDateTime.of(2026, 11, 11, 20, 0), null);
                venda.adicionarItem(new ItemVenda(venda, produto, 2));
                venda.adicionarItem(new ItemVenda(venda, produto, 2));

                view.atualizarVendas(List.of(venda));
                assertEquals(1, view.getTabelaVendas().getRowCount());
                view.getTabelaVendas().setRowSelectionInterval(0, 0);

                assertTrue(view.getAreaDetalhes().getText().contains("Café"));
                assertTrue(view.getAreaDetalhes().getText().contains("2 ×"));
                assertTrue(view.getAreaDetalhes().getText().contains("18,00"));

                view.atualizarVendas(List.of());
                assertEquals(0, view.getTabelaVendas().getRowCount());
                assertEquals("", view.getAreaDetalhes().getText());
            } catch (Throwable e) {
                falha.set(e);
            }
        });
        if (falha.get() != null) {
            throw new AssertionError("Falha ao validar a tela de histórico de vendas.", falha.get());
        }
    }

    @Test
    @DisplayName("Deve combinar filtros de período, produto e cliente")
    void deveCombinarFiltrosDoHistorico() throws Exception {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                HistoricoVendasView view = new HistoricoVendasView();
                Produto cafe = new Produto(8L, "HISTORY-8", "Café especial", new BigDecimal("4.50"), 2);
                Venda vendaAlvo = new Venda(
                        LocalDateTime.of(2026, 11, 11, 20, 0), new Cliente("Ana Silva"));
                vendaAlvo.adicionarItem(new ItemVenda(vendaAlvo, cafe, 2));
                Venda outroCliente = new Venda(
                        LocalDateTime.of(2026, 11, 11, 21, 0), new Cliente("Bruno"));
                outroCliente.adicionarItem(new ItemVenda(outroCliente, cafe, 1));
                Venda outraData = new Venda(
                        LocalDateTime.of(2026, 11, 12, 9, 0), new Cliente("Ana Silva"));
                outraData.adicionarItem(new ItemVenda(outraData, cafe, 1));

                view.atualizarVendas(List.of(vendaAlvo, outroCliente, outraData));
                view.getCampoDataInicial().setText("11/11/2026");
                view.getCampoDataFinal().setText("11/11/2026");
                view.getCampoProduto().setText("CAFÉ");
                view.getCampoCliente().setText("ana");

                assertEquals(1, view.getTabelaVendas().getRowCount());
                view.getTabelaVendas().setRowSelectionInterval(0, 0);
                assertTrue(view.getAreaDetalhes().getText().contains("Café especial"));
                assertTrue(view.getAreaDetalhes().getText().contains("9,00"));

                view.getBtnLimparFiltros().doClick();
                assertEquals(3, view.getTabelaVendas().getRowCount());
            } catch (Throwable e) {
                falha.set(e);
            }
        });
        if (falha.get() != null) {
            throw new AssertionError("Falha ao validar os filtros do histórico de vendas.", falha.get());
        }
    }

    @Test
    @DisplayName("Deve ocultar resultados e informar quando a data do filtro é inválida")
    void deveInformarDataInvalidaNoFiltro() throws Exception {
        AtomicReference<Throwable> falha = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                HistoricoVendasView view = new HistoricoVendasView();
                view.atualizarVendas(List.of(new Venda(LocalDateTime.of(2026, 11, 11, 20, 0), null)));

                view.getCampoDataInicial().setText("31/02/2026");

                assertEquals(0, view.getTabelaVendas().getRowCount());
                assertTrue(view.getStatusTexto().contains("Data inválida"));
            } catch (Throwable e) {
                falha.set(e);
            }
        });
        if (falha.get() != null) {
            throw new AssertionError("Falha ao validar a data no filtro do histórico.", falha.get());
        }
    }
}
