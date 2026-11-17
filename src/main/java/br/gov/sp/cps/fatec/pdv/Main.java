package br.gov.sp.cps.fatec.pdv;

import br.gov.sp.cps.fatec.pdv.controller.ProdutoController;
import br.gov.sp.cps.fatec.pdv.controller.HistoricoVendasController;
import br.gov.sp.cps.fatec.pdv.controller.VendaController;
import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.model.repository.ProdutoRepository;
import br.gov.sp.cps.fatec.pdv.model.repository.ProdutoRepositoryImpl;
import br.gov.sp.cps.fatec.pdv.model.repository.VendaRepositoryImpl;
import br.gov.sp.cps.fatec.pdv.model.service.ProdutoService;
import br.gov.sp.cps.fatec.pdv.model.service.VendaService;
import br.gov.sp.cps.fatec.pdv.util.JPAUtil;
import br.gov.sp.cps.fatec.pdv.view.MainFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ponto de Entrada Principal (Bootstrap) da Aplicação Desktop PDV-Swing-JPA.
 * <p>
 * <b>Conceito Pedagógico (Inversão de Controle & Thread Safety no Swing):</b>
 * <ul>
 * <li><b>Look & Feel Moderno:</b> Configura o tema FlatLaf para uma interface
 * moderna,
 * elegante e profissional, com fallback transparente para o tema nativo do
 * Sistema Operacional.</li>
 * <li><b>Event Dispatch Thread (EDT):</b> Toda criação e manipulação de
 * componentes gráficos
 * Swing deve ser executada obrigatoriamente dentro do método
 * {@link SwingUtilities#invokeLater(Runnable)},
 * garantindo segurança concorrente (Single-Thread Rule do Swing).</li>
 * <li><b>Injeção de Dependências Manual (Pure DI):</b> A classe {@code Main}
 * atua como
 * o <i>Composition Root</i>, instanciando o Repositório, o Serviço, a Visão e o
 * Controlador,
 * injetando as dependências de forma limpa sem a complexidade de frameworks
 * externos.</li>
 * <li><b>Ciclo de Vida Limpo:</b> Registra um
 * {@link java.awt.event.WindowListener} para encerrar
 * a conexão com o banco de dados H2 e o
 * {@link jakarta.persistence.EntityManagerFactory}
 * quando a janela for fechada pelo usuário.</li>
 * </ul>
 *
 * @author Cainã Antunes Silva
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static MainFrame mainFrame;

    public static void main(String[] args) {
        CountDownLatch inicializacaoConcluida = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            configurarLookAndFeel();

            new SwingWorker<ProdutoRepository, Void>() {
                @Override
                protected ProdutoRepository doInBackground() {
                    try {
                        prepararDiretorioDeDados();
                        ProdutoRepository repository = new ProdutoRepositoryImpl();
                        inicializarDadosDemonstracao(repository);
                        return repository;
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw t;
                    }
                }

                @Override
                protected void done() {
                    try {
                        iniciarInterface(get());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        tratarFalhaInicializacao(e);
                    } catch (ExecutionException e) {
                        tratarFalhaInicializacao(e.getCause());
                    } catch (Throwable t) {
                        tratarFalhaInicializacao(t);
                    } finally {
                        inicializacaoConcluida.countDown();
                    }
                }
            }.execute();
        });

        try {
            inicializacaoConcluida.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            SwingUtilities.invokeLater(() -> tratarFalhaInicializacao(e));
        }
    }

    /**
     * Aplica FlatLaf e usa o tema do sistema operacional somente se ele falhar.
     */
    private static void configurarLookAndFeel() {
        try {
            if (FlatLightLaf.setup()) {
                return;
            }
            LOGGER.warning("FlatLaf não pôde ser configurado; tentando o tema do sistema.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Falha ao configurar FlatLaf; tentando o tema do sistema.", e);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Não foi possível configurar FlatLaf nem o tema do sistema.", e);
        }
    }

    private static void iniciarInterface(ProdutoRepository repository) {
        mainFrame = new MainFrame();
        ProdutoService service = new ProdutoService(repository);
        ProdutoController controller = new ProdutoController(mainFrame, service);
        VendaService vendaService = new VendaService(new VendaRepositoryImpl());
        new VendaController(mainFrame, vendaService, service, controller::recarregarLista);
        new HistoricoVendasController(mainFrame, vendaService);
        mainFrame.getListView().getBtnHistoricoVendas().setEnabled(true);
        controller.inicializar();

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                LOGGER.info("Encerrando recursos de banco de dados JPA/H2.");
                JPAUtil.close();
            }
        });

        mainFrame.setVisible(true);
    }

    private static void prepararDiretorioDeDados() {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Não foi possível preparar o diretório de dados 'data' no diretório de execução.", e);
        }
    }

    private static void tratarFalhaInicializacao(Throwable erro) {
        LOGGER.log(Level.SEVERE, "Falha ao inicializar a aplicação ou preparar os dados.", erro);
        try {
            JPAUtil.close();
        } catch (RuntimeException erroFechamento) {
            LOGGER.log(Level.SEVERE, "Falha ao liberar os recursos JPA após erro de inicialização.", erroFechamento);
        }

        String detalhe = erro.getMessage() == null ? erro.getClass().getSimpleName() : erro.getMessage();
        JOptionPane.showMessageDialog(
                null,
                "Não foi possível iniciar a aplicação ou preparar o banco de dados.\n" + detalhe,
                "Falha na inicialização",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Popula o banco de dados com produtos de demonstração pedagógica se a base
     * estiver vazia. Erros são propagados para interromper a inicialização e
     * informar o usuário.
     *
     * @param repository Repositório de produtos.
     */
    private static void inicializarDadosDemonstracao(ProdutoRepository repository) {
        if (repository.listarTodos().isEmpty()) {
            LOGGER.info("Base vazia detectada. Inserindo dados de demonstração didática.");
            repository.salvar(new Produto("7891000100011", "Café Gourmet Especial Torrado 500g",
                    new BigDecimal("24.90"), 35));
            repository.salvar(
                    new Produto("7891000200022", "Açúcar Demerara Orgânico 1kg", new BigDecimal("6.50"), 80));
            repository.salvar(
                    new Produto("7891000300033", "Arroz Nobre Agulhinha Tipo 1 5kg", new BigDecimal("32.90"), 50));
            repository.salvar(
                    new Produto("7891000400044", "Feijão Carioca Selecionado 1kg", new BigDecimal("8.75"), 60));
            repository.salvar(new Produto("7891000500055", "Azeite de Oliva Extra Virgem 500ml",
                    new BigDecimal("41.50"), 25));
            LOGGER.info("Carga inicial de demonstração concluída.");
        }
    }
}

