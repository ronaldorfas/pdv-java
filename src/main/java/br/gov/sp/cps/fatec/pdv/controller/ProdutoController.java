package br.gov.sp.cps.fatec.pdv.controller;

import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.model.observer.ProdutoObserver;
import br.gov.sp.cps.fatec.pdv.model.repository.ProdutoRepository;
import br.gov.sp.cps.fatec.pdv.model.service.ProdutoService;
import br.gov.sp.cps.fatec.pdv.view.MainFrame;
import br.gov.sp.cps.fatec.pdv.view.ProdutoFormView;
import br.gov.sp.cps.fatec.pdv.view.ProdutoListView;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controlador Principal do Módulo de Gestão de Produtos (Padrão MVC &
 * Mediator).
 * <p>
 * <b>Conceito Pedagógico (Padrão Controller & Inversão de Dependência):</b>
 * <ul>
 * <li><b>Orquestrador Central:</b> O Controller captura as interações do
 * usuário disparadas nas telas Swing (eventos de botões), valida e converte
 * dados de entrada e aciona a camada de negócio em tarefas {@link SwingWorker},
 * mantendo chamadas de persistência fora da EDT.</li>
 * <li><b>Isolamento Completo:</b> A View nunca se comunica diretamente com o
 * Repositório ou banco de dados.</li>
 * <li><b>Inversão de Dependência (IoC):</b> O Controller recebe suas
 * dependências no construtor através
 * de interfaces e classes de serviço, desacoplando a camada de controle da
 * instanciação física.</li>
 * <li><b>Integração com o Padrão Observer:</b> O Controller implementa
 * {@link ProdutoObserver},
 * sendo notificado de forma reativa a cada persistência para atualizar a JTable
 * da tela.</li>
 * </ul>
 *
 * @author Cainã Antunes Silva
 */
public class ProdutoController implements ProdutoObserver {

    private static final Logger LOGGER = Logger.getLogger(ProdutoController.class.getName());

    private final MainFrame mainFrame;
    private final ProdutoListView listView;
    private final ProdutoFormView formView;
    private final ProdutoService service;
    private boolean operacaoEmAndamento;

    /**
     * Construtor recebendo o Repositório diretamente (Inversão de Dependência).
     *
     * @param mainFrame  Janela principal da aplicação.
     * @param repository Abstração do repositório de dados.
     */
    public ProdutoController(MainFrame mainFrame, ProdutoRepository repository) {
        this(mainFrame, new ProdutoService(repository));
    }

    /**
     * Construtor recebendo a camada de serviço.
     *
     * @param mainFrame Janela principal da aplicação.
     * @param service   Camada de serviços e regras de negócio.
     */
    public ProdutoController(MainFrame mainFrame, ProdutoService service) {
        this.mainFrame = Objects.requireNonNull(mainFrame, "MainFrame não pode ser nulo.");
        this.listView = mainFrame.getListView();
        this.formView = mainFrame.getFormView();
        this.service = Objects.requireNonNull(service, "ProdutoService não pode ser nulo.");

        vincularEventos();
    }

    /**
     * Inicializa o fluxo do controlador, conectando os observadores e carregando a
     * listagem inicial.
     */
    public void inicializar() {
        this.service.addObserver(this);
        this.mainFrame.mostrarLista();
        executarOperacao(
                "carregar produtos",
                service::listarTodos,
                this::onProdutosChanged);
    }

    /**
     * Configura os ouvintes de eventos (Listeners) nos componentes visuais das
     * telas.
     */
    private void vincularEventos() {
        // --- Eventos da Tela de Listagem ---
        listView.getBtnNovo().addActionListener(e -> onNovoProduto());
        listView.getBtnEditar().addActionListener(e -> onEditarProduto());
        listView.getBtnExcluir().addActionListener(e -> onExcluirProduto());
        listView.getBtnRecarregar().addActionListener(e -> onRecarregarProdutos());

        // --- Eventos da Tela de Formulário ---
        formView.getBtnSalvar().addActionListener(e -> onSalvarProduto());
        formView.getBtnLimpar().addActionListener(e -> onLimparFormulario());
        formView.getBtnVoltar().addActionListener(e -> onVoltarParaListagem());
    }

    // ==========================================
    // TRATADORES DE EVENTOS (HANDLERS)
    // ==========================================

    private void onNovoProduto() {
        if (operacaoEmAndamento) {
            return;
        }
        formView.configurarModoNovo();
        mainFrame.mostrarFormulario();
    }

    private void onVoltarParaListagem() {
        if (operacaoEmAndamento) {
            return;
        }
        mainFrame.mostrarLista();
    }

    private void onLimparFormulario() {
        if (operacaoEmAndamento) {
            return;
        }
        formView.limparCampos();
    }

    private void onRecarregarProdutos() {
        executarOperacao("recarregar produtos", service::listarTodos, this::onProdutosChanged);
    }

    public void recarregarLista() {
        onRecarregarProdutos();
    }

    private void onEditarProduto() {
        if (operacaoEmAndamento) {
            return;
        }
        Produto selecionado = listView.getProdutoSelecionado();
        if (selecionado == null) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Selecione um produto na tabela para editar.",
                    "Atenção - Seleção Necessária",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        formView.carregarParaEdicao(selecionado);
        mainFrame.mostrarFormulario();
    }

    private void onExcluirProduto() {
        if (operacaoEmAndamento) {
            return;
        }
        Produto selecionado = listView.getProdutoSelecionado();
        if (selecionado == null) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    "Selecione um produto na tabela para excluir.",
                    "Atenção - Seleção Necessária",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(
                mainFrame,
                String.format("Deseja realmente excluir o produto abaixo?\n\nID: #%d\nNome: %s\nCódigo: %s",
                        selecionado.getId(), selecionado.getNome(), selecionado.getCodigoBarras()),
                "Confirmar Exclusão de Produto",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirmacao == JOptionPane.YES_OPTION) {
            executarOperacao("excluir produto", () -> {
                service.excluir(selecionado.getId());
                return null;
            }, resultado -> {
                JOptionPane.showMessageDialog(
                        mainFrame,
                        "Produto excluído com sucesso!",
                        "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
            });
        }
    }

    private void onSalvarProduto() {
        if (operacaoEmAndamento) {
            return;
        }
        try {
            String codigoBarras = formView.getCodigoBarras();
            String nome = formView.getNome();
            BigDecimal preco = parsePreco(formView.getPrecoTexto());
            Integer quantidadeEstoque = parseQuantidadeEstoque(formView.getQuantidadeEstoqueTexto());

            Long id = formView.getProdutoIdAtual();

            if (id == null) {
                Produto novoProduto = new Produto(codigoBarras, nome, preco, quantidadeEstoque);
                executarOperacao("cadastrar produto", () -> service.salvar(novoProduto), resultado -> {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Produto cadastrado com sucesso!",
                            "Sucesso",
                            JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.mostrarLista();
                });
            } else {
                Produto produtoAtualizado = new Produto(id, codigoBarras, nome, preco, quantidadeEstoque);
                executarOperacao("atualizar produto", () -> service.atualizar(produtoAtualizado), resultado -> {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "Produto atualizado com sucesso!",
                            "Sucesso",
                            JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.mostrarLista();
                });
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    mainFrame,
                    ex.getMessage(),
                    "Validação de Dados",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    static BigDecimal parsePreco(String texto) {
        String valor = texto == null ? "" : texto.trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("Informe o preço do produto.");
        }
        if (!valor.matches("\\d+(?:[.,]\\d{1,2})?")) {
            throw new IllegalArgumentException(
                    "Preço inválido. Informe um valor positivo com até duas casas decimais (ex.: 19,99).");
        }
        return new BigDecimal(valor.replace(',', '.'));
    }

    static Integer parseQuantidadeEstoque(String texto) {
        String valor = texto == null ? "" : texto.trim();
        if (valor.isEmpty()) {
            throw new IllegalArgumentException("Informe a quantidade em estoque.");
        }
        if (!valor.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "Quantidade em estoque inválida. Informe um número inteiro entre 0 e 2.147.483.647.");
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Quantidade em estoque inválida. Informe um número inteiro entre 0 e 2.147.483.647.", e);
        }
    }

    private <T> void executarOperacao(String descricao, Callable<T> tarefa, Consumer<T> aoConcluir) {
        if (operacaoEmAndamento) {
            return;
        }
        operacaoEmAndamento = true;
        setBotoesOperacionaisEnabled(false);

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarefa.call();
            }

            @Override
            protected void done() {
                operacaoEmAndamento = false;
                setBotoesOperacionaisEnabled(true);

                final T resultado;
                try {
                    resultado = get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exibirFalhaOperacao(descricao, e);
                    return;
                } catch (ExecutionException e) {
                    exibirFalhaOperacao(descricao, e.getCause());
                    return;
                }

                aoConcluir.accept(resultado);
            }
        }.execute();
    }

    private void setBotoesOperacionaisEnabled(boolean enabled) {
        listView.getBtnNovo().setEnabled(enabled);
        listView.getBtnEditar().setEnabled(enabled);
        listView.getBtnExcluir().setEnabled(enabled);
        listView.getBtnRecarregar().setEnabled(enabled);
        listView.getBtnNovaVenda().setEnabled(enabled);
        formView.getBtnSalvar().setEnabled(enabled);
        formView.getBtnLimpar().setEnabled(enabled);
        formView.getBtnVoltar().setEnabled(enabled);
    }

    private void exibirFalhaOperacao(String descricao, Throwable erro) {
        LOGGER.log(Level.SEVERE, "Falha ao " + descricao + ".", erro);
        String detalhe = erro.getMessage() == null ? erro.getClass().getSimpleName() : erro.getMessage();
        boolean erroDeValidacao = erro instanceof IllegalArgumentException;
        JOptionPane.showMessageDialog(
                mainFrame,
                "Não foi possível " + descricao + ".\n" + detalhe,
                erroDeValidacao ? "Validação de Dados" : "Falha na operação",
                erroDeValidacao ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    // ==========================================
    // IMPLEMENTAÇÃO DO OBSERVER PATTERN
    // ==========================================

    @Override
    public void onProdutosChanged(List<Produto> produtos) {
        // Atualiza a tabela na View quando o Modelo/Service sinaliza alterações
        listView.atualizarTabela(produtos);
    }
}
