package br.gov.sp.cps.fatec.pdv.controller;

import br.gov.sp.cps.fatec.pdv.model.entity.Venda;
import br.gov.sp.cps.fatec.pdv.model.service.VendaService;
import br.gov.sp.cps.fatec.pdv.view.HistoricoVendasView;
import br.gov.sp.cps.fatec.pdv.view.MainFrame;

import javax.swing.SwingWorker;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HistoricoVendasController {

    private static final Logger LOGGER = Logger.getLogger(HistoricoVendasController.class.getName());

    private final MainFrame mainFrame;
    private final HistoricoVendasView view;
    private final VendaService service;
    private boolean carregando;

    public HistoricoVendasController(MainFrame mainFrame, VendaService service) {
        this.mainFrame = Objects.requireNonNull(mainFrame, "MainFrame não pode ser nulo.");
        this.view = mainFrame.getHistoricoVendasView();
        this.service = Objects.requireNonNull(service, "VendaService não pode ser nulo.");

        mainFrame.getListView().getBtnHistoricoVendas().addActionListener(e -> abrirHistorico());
        view.getBtnAtualizar().addActionListener(e -> carregarVendas());
        view.getBtnVoltar().addActionListener(e -> {
            if (!carregando) {
                mainFrame.mostrarLista();
            }
        });
    }

    private void abrirHistorico() {
        if (carregando) {
            return;
        }
        mainFrame.mostrarHistoricoVendas();
        carregarVendas();
    }

    private void carregarVendas() {
        if (carregando) {
            return;
        }
        carregando = true;
        view.getBtnAtualizar().setEnabled(false);
        view.getBtnVoltar().setEnabled(false);

        new SwingWorker<List<Venda>, Void>() {
            @Override
            protected List<Venda> doInBackground() {
                return service.listarTodas();
            }

            @Override
            protected void done() {
                carregando = false;
                view.getBtnAtualizar().setEnabled(true);
                view.getBtnVoltar().setEnabled(true);
                try {
                    view.atualizarVendas(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    falhar(e);
                } catch (ExecutionException e) {
                    falhar(e.getCause());
                }
            }
        }.execute();
    }

    private void falhar(Throwable erro) {
        LOGGER.log(Level.SEVERE, "Falha ao carregar o histórico de vendas.", erro);
        String mensagem = erro.getMessage() == null ? erro.getClass().getSimpleName() : erro.getMessage();
        view.exibirErro(mensagem);
    }
}
