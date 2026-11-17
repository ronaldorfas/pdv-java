package br.gov.sp.cps.fatec.pdv.controller;

import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.model.entity.Venda;
import br.gov.sp.cps.fatec.pdv.model.repository.ItemVendaSolicitado;
import br.gov.sp.cps.fatec.pdv.model.service.ProdutoService;
import br.gov.sp.cps.fatec.pdv.model.service.VendaService;
import br.gov.sp.cps.fatec.pdv.view.MainFrame;
import br.gov.sp.cps.fatec.pdv.view.VendaView;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VendaController {

    private static final Logger LOGGER = Logger.getLogger(VendaController.class.getName());

    private final MainFrame mainFrame;
    private final VendaView view;
    private final VendaService service;
    private final ProdutoService produtoService;
    private final Runnable aoConcluirVenda;
    private boolean operacaoEmAndamento;

    public VendaController(
            MainFrame mainFrame,
            VendaService service,
            ProdutoService produtoService,
            Runnable aoConcluirVenda) {
        this.mainFrame = Objects.requireNonNull(mainFrame, "MainFrame não pode ser nulo.");
        this.view = mainFrame.getVendaView();
        this.service = Objects.requireNonNull(service, "VendaService não pode ser nulo.");
        this.produtoService = Objects.requireNonNull(produtoService, "ProdutoService não pode ser nulo.");
        this.aoConcluirVenda = Objects.requireNonNull(aoConcluirVenda, "Callback de conclusão não pode ser nulo.");

        mainFrame.getListView().getBtnNovaVenda().addActionListener(e -> {
            if (!operacaoEmAndamento) {
                iniciarNovaVenda();
            }
        });
        view.getBtnAdicionarItem().addActionListener(e -> adicionarItem());
        view.getBtnRemoverItem().addActionListener(e -> removerItem());
        view.getBtnFinalizar().addActionListener(e -> finalizarVenda());
        view.getBtnVoltar().addActionListener(e -> {
            if (!operacaoEmAndamento) {
                mainFrame.mostrarLista();
            }
        });
    }

    private void adicionarItem() {
        if (operacaoEmAndamento) {
            return;
        }
        String codigo = view.getCodigoBarras();
        if (codigo.isEmpty() && view.getProdutoSelecionado() != null) {
            codigo = view.getProdutoSelecionado().getCodigoBarras();
        }
        if (codigo.isEmpty()) {
            mostrarValidacao("Informe o código de barras ou selecione um produto na lista.");
            return;
        }
        Produto produto = view.buscarProdutoPorCodigoBarras(codigo);
        if (produto == null) {
            mostrarValidacao("Produto não encontrado no catálogo.");
            return;
        }

        final int quantidade;
        try {
            quantidade = Integer.parseInt(view.getQuantidadeTexto());
        } catch (NumberFormatException e) {
            mostrarValidacao("Informe uma quantidade inteira maior que zero.");
            return;
        }
        if (quantidade <= 0) {
            mostrarValidacao("Informe uma quantidade inteira maior que zero.");
            return;
        }
        view.adicionarItem(new ItemVendaSolicitado(codigo, quantidade), produto);
    }

    private void iniciarNovaVenda() {
        view.limparVenda();
        view.atualizarProdutos(List.of());
        mainFrame.mostrarVenda();
        operacaoEmAndamento = true;
        setBotoesEnabled(false);

        new SwingWorker<List<Produto>, Void>() {
            @Override
            protected List<Produto> doInBackground() {
                return produtoService.listarTodos();
            }

            @Override
            protected void done() {
                operacaoEmAndamento = false;
                setBotoesEnabled(true);
                try {
                    view.atualizarProdutos(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarFalhaCarregamentoProdutos(e);
                } catch (ExecutionException e) {
                    mostrarFalhaCarregamentoProdutos(e.getCause());
                }
            }
        }.execute();
    }

    private void removerItem() {
        if (!operacaoEmAndamento && !view.removerItemSelecionado()) {
            mostrarValidacao("Selecione um item da venda para removê-lo.");
        }
    }

    private void finalizarVenda() {
        if (operacaoEmAndamento) {
            return;
        }
        String nomeCliente = view.getNomeCliente();
        List<ItemVendaSolicitado> itens = view.getItensSolicitados();
        operacaoEmAndamento = true;
        setBotoesEnabled(false);

        new SwingWorker<Venda, Void>() {
            @Override
            protected Venda doInBackground() {
                return service.registrar(nomeCliente, itens);
            }

            @Override
            protected void done() {
                operacaoEmAndamento = false;
                setBotoesEnabled(true);
                try {
                    Venda venda = get();
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            String.format("Venda #%d registrada com sucesso.%nTotal: %s",
                                    venda.getId(),
                                    java.text.NumberFormat.getCurrencyInstance(
                                            java.util.Locale.forLanguageTag("pt-BR"))
                                            .format(venda.getValorTotal())),
                            "Venda concluída",
                            JOptionPane.INFORMATION_MESSAGE);
                    view.limparVenda();
                    mainFrame.mostrarLista();
                    aoConcluirVenda.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarFalha(e);
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    if (causa instanceof IllegalArgumentException) {
                        mostrarValidacao(causa.getMessage());
                    } else {
                        mostrarFalha(causa);
                    }
                }
            }
        }.execute();
    }

    private void setBotoesEnabled(boolean enabled) {
        view.getBtnAdicionarItem().setEnabled(enabled);
        view.getBtnRemoverItem().setEnabled(enabled);
        view.getBtnFinalizar().setEnabled(enabled);
        view.getBtnVoltar().setEnabled(enabled);
    }

    private void mostrarValidacao(String mensagem) {
        JOptionPane.showMessageDialog(
                mainFrame,
                mensagem,
                "Validação da venda",
                JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarFalha(Throwable erro) {
        LOGGER.log(Level.SEVERE, "Falha ao registrar venda.", erro);
        String detalhe = erro.getMessage() == null ? erro.getClass().getSimpleName() : erro.getMessage();
        JOptionPane.showMessageDialog(
                mainFrame,
                "Não foi possível registrar a venda. Nenhuma alteração foi confirmada.\n" + detalhe,
                "Falha na venda",
                JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarFalhaCarregamentoProdutos(Throwable erro) {
        LOGGER.log(Level.SEVERE, "Falha ao carregar produtos para a venda.", erro);
        String detalhe = erro.getMessage() == null ? erro.getClass().getSimpleName() : erro.getMessage();
        JOptionPane.showMessageDialog(
                mainFrame,
                "Não foi possível carregar os produtos para a venda.\n" + detalhe,
                "Falha na venda",
                JOptionPane.ERROR_MESSAGE);
    }
}
