package br.gov.sp.cps.fatec.pdv.view;

import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.view.tablemodel.ProdutoTableModel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Visão (View) da Listagem de Produtos em formato tabular ({@link JTable}).
 * <p>
 * <b>Conceito Pedagógico (Padrão MVC & Componentes Reativos do Swing):</b>
 * <ul>
 * <li>Apresenta a listagem atualizada de produtos persistidos no banco de
 * dados.</li>
 * <li>O desacoplamento é garantido através do {@link ProdutoTableModel}: a
 * tabela é atualizada
 * automaticamente sempre que novas listas são fornecidas pelo Controller.</li>
 * <li>Expõe botões de comando ("Novo Produto", "Editar", "Excluir") e método
 * para identificar
 * o registro atualmente selecionado pelo usuário.</li>
 * </ul>
 *
 * @author Cainã Antunes Silva
 */
public class ProdutoListView extends JPanel {

    private final JLabel lblTitulo;
    private final JLabel lblTotalRegistros;
    private final JTable tabelaProdutos;
    private final ProdutoTableModel tableModel;
    private final TableRowSorter<ProdutoTableModel> sorter;
    private final JTextField campoBusca;

    private final JButton btnNovo;
    private final JButton btnEditar;
    private final JButton btnExcluir;
    private final JButton btnRecarregar;
    private final JButton btnLimparBusca;
    private final JButton btnNovaVenda;
    private final JButton btnHistoricoVendas;

    public ProdutoListView() {
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // 1. Cabeçalho Didático
        JPanel painelCabecalho = new JPanel();
        painelCabecalho.setLayout(new BoxLayout(painelCabecalho, BoxLayout.Y_AXIS));

        lblTitulo = new JLabel("Catálogo de Produtos - PDV");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel lblSubtitulo = new JLabel("Visualização em tempo real dos produtos persistidos no banco de dados.");
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtitulo.setForeground(Color.GRAY);

        JPanel painelBusca = new JPanel(new BorderLayout(8, 0));
        JLabel lblBusca = new JLabel("Buscar por nome ou código:");
        campoBusca = new JTextField();
        campoBusca.setToolTipText("Digite parte do nome ou do código de barras para filtrar a tabela.");
        btnLimparBusca = new JButton("Limpar busca");
        painelBusca.add(lblBusca, BorderLayout.WEST);
        painelBusca.add(campoBusca, BorderLayout.CENTER);
        painelBusca.add(btnLimparBusca, BorderLayout.EAST);

        JPanel painelTopo = new JPanel(new BorderLayout(0, 12));
        painelCabecalho.add(lblTitulo);
        painelCabecalho.add(Box.createVerticalStrut(4));
        painelCabecalho.add(lblSubtitulo);
        painelTopo.add(painelCabecalho, BorderLayout.NORTH);
        painelTopo.add(painelBusca, BorderLayout.SOUTH);
        add(painelTopo, BorderLayout.NORTH);

        // 2. Tabela central com ordenação por cabeçalho e filtro de busca
        tableModel = new ProdutoTableModel();
        tabelaProdutos = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        tabelaProdutos.setRowSorter(sorter);
        tabelaProdutos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaProdutos.setRowHeight(28);
        tabelaProdutos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabelaProdutos.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabelaProdutos.getTableHeader().setReorderingAllowed(false);

        // Ajuste didático de largura das colunas
        tabelaProdutos.getColumnModel().getColumn(0).setPreferredWidth(60); // ID
        tabelaProdutos.getColumnModel().getColumn(1).setPreferredWidth(140); // Código de Barras
        tabelaProdutos.getColumnModel().getColumn(2).setPreferredWidth(280); // Nome
        tabelaProdutos.getColumnModel().getColumn(3).setPreferredWidth(120); // Preço
        tabelaProdutos.getColumnModel().getColumn(4).setPreferredWidth(100); // Quantidade

        JScrollPane scrollPane = new JScrollPane(tabelaProdutos);
        scrollPane.setBorder(BorderFactory.createTitledBorder(" Produtos Cadastrados "));
        add(scrollPane, BorderLayout.CENTER);

        campoBusca.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                aplicarFiltroBusca();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                aplicarFiltroBusca();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                aplicarFiltroBusca();
            }
        });
        btnLimparBusca.addActionListener(e -> campoBusca.setText(""));
        sorter.addRowSorterListener(e -> atualizarContador());

        // 3. Rodapé com ações e contador de registros
        JPanel painelRodape = new JPanel(new BorderLayout(10, 10));

        lblTotalRegistros = new JLabel();
        lblTotalRegistros.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblTotalRegistros.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        atualizarContador();
        painelRodape.add(lblTotalRegistros, BorderLayout.WEST);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        btnRecarregar = new JButton("Recarregar");
        btnRecarregar.setPreferredSize(new Dimension(110, 35));

        btnExcluir = new JButton("Excluir");
        btnExcluir.setPreferredSize(new Dimension(110, 35));
        btnExcluir.setForeground(new Color(180, 40, 40));

        btnEditar = new JButton("Editar");
        btnEditar.setPreferredSize(new Dimension(110, 35));

        btnNovo = new JButton("+ Novo Produto");
        btnNovo.setPreferredSize(new Dimension(150, 35));
        btnNovo.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnNovaVenda = new JButton("Nova Venda");
        btnNovaVenda.setPreferredSize(new Dimension(130, 35));
        btnHistoricoVendas = new JButton("Histórico de vendas");
        btnHistoricoVendas.setPreferredSize(new Dimension(160, 35));

        painelBotoes.add(btnRecarregar);
        painelBotoes.add(btnExcluir);
        painelBotoes.add(btnEditar);
        painelBotoes.add(btnNovaVenda);
        painelBotoes.add(btnHistoricoVendas);
        painelBotoes.add(btnNovo);

        painelRodape.add(painelBotoes, BorderLayout.EAST);
        add(painelRodape, BorderLayout.SOUTH);
    }

    /**
     * Atualiza a tabela com a lista atual de produtos e renova o contador do
     * rodapé.
     *
     * @param produtos Coleção de produtos a exibir.
     */
    public void atualizarTabela(List<Produto> produtos) {
        tabelaProdutos.clearSelection();
        tableModel.setProdutos(produtos);
        atualizarContador();
    }

    private void aplicarFiltroBusca() {
        tabelaProdutos.clearSelection();
        String termo = campoBusca.getText().trim();
        if (termo.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            RowFilter<ProdutoTableModel, Integer> filtro = RowFilter.regexFilter(
                    "(?iu)" + Pattern.quote(termo), 1, 2);
            sorter.setRowFilter(filtro);
        }
        atualizarContador();
    }

    private void atualizarContador() {
        int exibidos = tabelaProdutos == null ? 0 : tabelaProdutos.getRowCount();
        int total = tableModel == null ? 0 : tableModel.getRowCount();
        lblTotalRegistros.setText(String.format("Exibindo %d de %d produto(s)", exibidos, total));
    }

    /**
     * Retorna a entidade {@link Produto} selecionada na linha ativa da tabela.
     *
     * @return {@link Produto} selecionado ou {@code null} se nenhuma linha estiver
     *         selecionada.
     */
    public Produto getProdutoSelecionado() {
        int selectedRow = tabelaProdutos.getSelectedRow();
        if (selectedRow >= 0) {
            return tableModel.getProdutoAt(tabelaProdutos.convertRowIndexToModel(selectedRow));
        }
        return null;
    }

    // ==========================================
    // GETTERS PARA COMPONENTES E OUVINTES
    // ==========================================

    public JTable getTabelaProdutos() {
        return tabelaProdutos;
    }

    public ProdutoTableModel getTableModel() {
        return tableModel;
    }

    public JTextField getCampoBusca() {
        return campoBusca;
    }

    public JButton getBtnLimparBusca() {
        return btnLimparBusca;
    }

    public JButton getBtnNovo() {
        return btnNovo;
    }

    public JButton getBtnEditar() {
        return btnEditar;
    }

    public JButton getBtnExcluir() {
        return btnExcluir;
    }

    public JButton getBtnRecarregar() {
        return btnRecarregar;
    }

    public JButton getBtnNovaVenda() {
        return btnNovaVenda;
    }

    public JButton getBtnHistoricoVendas() {
        return btnHistoricoVendas;
    }
}
