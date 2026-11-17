package br.gov.sp.cps.fatec.pdv.view;

import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.model.repository.ItemVendaSolicitado;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;

public class VendaView extends JPanel {

    private final JTextField campoNomeCliente;
    private final JTextField campoCodigoBarras;
    private final JTextField campoQuantidade;
    private final JTextField campoBuscaProduto;
    private final DefaultListModel<Produto> modeloProdutos;
    private final JList<Produto> listaProdutos;
    private List<Produto> produtosDisponiveis = List.of();
    private final DefaultTableModel modeloItens;
    private final JTable tabelaItens;
    private final List<ItemVendaSolicitado> itens;
    private final List<Produto> produtosDosItens;
    private final JLabel lblValorParcial;
    private final JButton btnAdicionarItem;
    private final JButton btnRemoverItem;
    private final JButton btnFinalizar;
    private final JButton btnVoltar;

    public VendaView() {
        this.itens = new ArrayList<>();
        this.produtosDosItens = new ArrayList<>();
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel painelCampos = new JPanel(new java.awt.GridLayout(2, 3, 8, 6));
        painelCampos.setBorder(BorderFactory.createTitledBorder("Dados da venda"));
        campoNomeCliente = new JTextField();
        campoNomeCliente.setToolTipText("Opcional. Até 120 caracteres.");
        campoCodigoBarras = new JTextField();
        campoQuantidade = new JTextField("1");
        painelCampos.add(new JLabel("Cliente (opcional):"));
        painelCampos.add(new JLabel("Código de barras:"));
        painelCampos.add(new JLabel("Quantidade:"));
        painelCampos.add(campoNomeCliente);
        painelCampos.add(campoCodigoBarras);
        painelCampos.add(campoQuantidade);

        JPanel painelTopo = new JPanel(new BorderLayout(0, 10));
        painelTopo.add(new JLabel("Registrar Venda"), BorderLayout.NORTH);
        painelTopo.add(painelCampos, BorderLayout.CENTER);

        btnAdicionarItem = new JButton("Adicionar produto");
        JPanel painelAdicionar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelAdicionar.add(btnAdicionarItem);
        painelTopo.add(painelAdicionar, BorderLayout.SOUTH);
        add(painelTopo, BorderLayout.NORTH);

        campoBuscaProduto = new JTextField();
        campoBuscaProduto.setToolTipText("Digite parte do nome para localizar um produto.");
        modeloProdutos = new DefaultListModel<>();
        listaProdutos = new JList<>(modeloProdutos);
        listaProdutos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaProdutos.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                JLabel celula = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                Produto produto = (Produto) value;
                celula.setText(String.format(
                        "%s  |  %s  |  Estoque: %d",
                        produto.getNome(),
                        java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
                                .format(produto.getPreco()),
                        produto.getQuantidadeEstoque()));
                return celula;
            }
        });
        JPanel painelBuscaProduto = new JPanel(new BorderLayout(0, 6));
        painelBuscaProduto.setBorder(BorderFactory.createTitledBorder("Buscar produto por nome"));
        painelBuscaProduto.add(campoBuscaProduto, BorderLayout.NORTH);
        painelBuscaProduto.add(new JScrollPane(listaProdutos), BorderLayout.CENTER);

        modeloItens = new DefaultTableModel(
                new Object[] { "Produto", "Quantidade", "Preço unitário", "Parcial" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaItens = new JTable(modeloItens);
        tabelaItens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel painelItens = new JPanel(new BorderLayout(0, 6));
        painelItens.add(new JLabel("Preço e estoque são conferidos no banco ao finalizar a venda."),
                BorderLayout.NORTH);
        painelItens.add(new JScrollPane(tabelaItens), BorderLayout.CENTER);

        lblValorParcial = new JLabel();
        lblValorParcial.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
        lblValorParcial.setHorizontalAlignment(JLabel.RIGHT);
        painelItens.add(lblValorParcial, BorderLayout.SOUTH);

        JSplitPane painelCentral = new JSplitPane(
                javax.swing.JSplitPane.HORIZONTAL_SPLIT, painelBuscaProduto, painelItens);
        painelCentral.setResizeWeight(0.45);
        add(painelCentral, BorderLayout.CENTER);
        atualizarValorParcial();

        campoBuscaProduto.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                aplicarFiltroProdutos();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                aplicarFiltroProdutos();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                aplicarFiltroProdutos();
            }
        });

        btnRemoverItem = new JButton("Remover item");
        btnVoltar = new JButton("Voltar ao catálogo");
        btnFinalizar = new JButton("Finalizar venda");
        JPanel painelAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelAcoes.add(btnVoltar);
        painelAcoes.add(btnRemoverItem);
        painelAcoes.add(btnFinalizar);
        add(painelAcoes, BorderLayout.SOUTH);
    }

    public void adicionarItem(ItemVendaSolicitado item, Produto produto) {
        itens.add(item);
        produtosDosItens.add(produto);
        modeloItens.addRow(new Object[] {
                produto.getNome(),
                item.quantidade(),
                formatarMoeda(produto.getPreco()),
                formatarMoeda(produto.getPreco().multiply(BigDecimal.valueOf(item.quantidade())))
        });
        atualizarValorParcial();
        campoCodigoBarras.setText("");
        campoQuantidade.setText("1");
        listaProdutos.clearSelection();
        campoCodigoBarras.requestFocusInWindow();
    }

    public void atualizarProdutos(List<Produto> produtos) {
        produtosDisponiveis = List.copyOf(produtos);
        aplicarFiltroProdutos();
    }

    private void aplicarFiltroProdutos() {
        String termo = campoBuscaProduto.getText().trim().toLowerCase(Locale.ROOT);
        modeloProdutos.clear();
        for (Produto produto : produtosDisponiveis) {
            if (produto.getNome().toLowerCase(Locale.ROOT).contains(termo)) {
                modeloProdutos.addElement(produto);
            }
        }
    }

    public boolean removerItemSelecionado() {
        int linha = tabelaItens.getSelectedRow();
        if (linha < 0) {
            return false;
        }
        itens.remove(linha);
        produtosDosItens.remove(linha);
        modeloItens.removeRow(linha);
        atualizarValorParcial();
        return true;
    }

    private void atualizarValorParcial() {
        lblValorParcial.setText("Valor parcial: " + formatarMoeda(getValorParcial()));
    }

    private String formatarMoeda(BigDecimal valor) {
        return java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
                .format(valor);
    }

    public BigDecimal getValorParcial() {
        BigDecimal parcial = BigDecimal.ZERO;
        for (int i = 0; i < itens.size(); i++) {
            parcial = parcial.add(produtosDosItens.get(i).getPreco()
                    .multiply(BigDecimal.valueOf(itens.get(i).quantidade())));
        }
        return parcial;
    }

    public Produto buscarProdutoPorCodigoBarras(String codigoBarras) {
        return produtosDisponiveis.stream()
                .filter(produto -> produto.getCodigoBarras().equals(codigoBarras))
                .findFirst()
                .orElse(null);
    }

    public List<ItemVendaSolicitado> getItensSolicitados() {
        return List.copyOf(itens);
    }

    public String getNomeCliente() {
        return campoNomeCliente.getText().trim();
    }

    public String getCodigoBarras() {
        return campoCodigoBarras.getText().trim();
    }

    public String getQuantidadeTexto() {
        return campoQuantidade.getText().trim();
    }

    public Produto getProdutoSelecionado() {
        return listaProdutos.getSelectedValue();
    }

    public JTextField getCampoBuscaProduto() {
        return campoBuscaProduto;
    }

    public JList<Produto> getListaProdutos() {
        return listaProdutos;
    }

    public void limparVenda() {
        itens.clear();
        produtosDosItens.clear();
        modeloItens.setRowCount(0);
        campoNomeCliente.setText("");
        campoCodigoBarras.setText("");
        campoQuantidade.setText("1");
        campoBuscaProduto.setText("");
        listaProdutos.clearSelection();
        atualizarValorParcial();
    }

    public JButton getBtnAdicionarItem() {
        return btnAdicionarItem;
    }

    public JButton getBtnRemoverItem() {
        return btnRemoverItem;
    }

    public JButton getBtnFinalizar() {
        return btnFinalizar;
    }

    public JButton getBtnVoltar() {
        return btnVoltar;
    }

    public JTable getTabelaItens() {
        return tabelaItens;
    }

    public JLabel getLblValorParcial() {
        return lblValorParcial;
    }
}
