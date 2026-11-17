package br.gov.sp.cps.fatec.pdv.view;

import br.gov.sp.cps.fatec.pdv.model.entity.ItemVenda;
import br.gov.sp.cps.fatec.pdv.model.entity.Venda;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HistoricoVendasView extends JPanel {

    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA_FILTRO_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final NumberFormat MOEDA_FORMATTER =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private final DefaultTableModel tableModel;
    private final JTable tabelaVendas;
    private final JTextArea areaDetalhes;
    private final JLabel lblStatus;
    private final JTextField campoDataInicial;
    private final JTextField campoDataFinal;
    private final JTextField campoProduto;
    private final JTextField campoCliente;
    private final JButton btnLimparFiltros;
    private final JButton btnAtualizar;
    private final JButton btnVoltar;
    private List<Venda> vendas = List.of();
    private List<Venda> vendasExibidas = List.of();

    public HistoricoVendasView() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        JPanel painelTopo = new JPanel(new BorderLayout());
        painelTopo.add(new JLabel("Histórico de Vendas"), BorderLayout.WEST);
        lblStatus = new JLabel("Carregue as vendas para consultar o histórico.");
        painelTopo.add(lblStatus, BorderLayout.EAST);
        JPanel painelFiltros = new JPanel(new java.awt.GridLayout(2, 4, 8, 4));
        painelFiltros.setBorder(BorderFactory.createTitledBorder("Filtros (combináveis)"));
        campoDataInicial = new JTextField();
        campoDataFinal = new JTextField();
        campoProduto = new JTextField();
        campoCliente = new JTextField();
        campoDataInicial.setToolTipText("Data inicial no formato dd/MM/aaaa.");
        campoDataFinal.setToolTipText("Data final no formato dd/MM/aaaa.");
        painelFiltros.add(new JLabel("Data inicial (dd/MM/aaaa):"));
        painelFiltros.add(new JLabel("Data final (dd/MM/aaaa):"));
        painelFiltros.add(new JLabel("Produto:"));
        painelFiltros.add(new JLabel("Cliente:"));
        painelFiltros.add(campoDataInicial);
        painelFiltros.add(campoDataFinal);
        painelFiltros.add(campoProduto);
        painelFiltros.add(campoCliente);
        btnLimparFiltros = new JButton("Limpar filtros");
        JPanel painelFiltrosAcao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        painelFiltrosAcao.add(btnLimparFiltros);
        JPanel painelSuperior = new JPanel(new BorderLayout(0, 6));
        painelSuperior.add(painelTopo, BorderLayout.NORTH);
        painelSuperior.add(painelFiltros, BorderLayout.CENTER);
        painelSuperior.add(painelFiltrosAcao, BorderLayout.SOUTH);
        add(painelSuperior, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[] { "Venda", "Data e hora", "Cliente", "Total" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaVendas = new JTable(tableModel);
        tabelaVendas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaVendas.setRowHeight(26);
        tabelaVendas.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabelaVendas.getColumnModel().getColumn(1).setPreferredWidth(150);
        tabelaVendas.getColumnModel().getColumn(2).setPreferredWidth(260);
        tabelaVendas.getColumnModel().getColumn(3).setPreferredWidth(130);
        JPanel painelConteudo = new JPanel(new BorderLayout(0, 8));
        painelConteudo.add(new JScrollPane(tabelaVendas), BorderLayout.CENTER);

        areaDetalhes = new JTextArea(5, 40);
        areaDetalhes.setEditable(false);
        areaDetalhes.setLineWrap(true);
        areaDetalhes.setWrapStyleWord(true);
        areaDetalhes.setBorder(BorderFactory.createTitledBorder("Itens da venda selecionada"));
        JScrollPane scrollDetalhes = new JScrollPane(areaDetalhes);
        scrollDetalhes.setPreferredSize(new Dimension(400, 130));
        painelConteudo.add(scrollDetalhes, BorderLayout.SOUTH);
        add(painelConteudo, BorderLayout.CENTER);

        btnAtualizar = new JButton("Atualizar histórico");
        btnVoltar = new JButton("Voltar ao catálogo");
        JPanel painelAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        painelAcoes.add(btnVoltar);
        painelAcoes.add(btnAtualizar);
        add(painelAcoes, BorderLayout.SOUTH);

        tabelaVendas.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                atualizarDetalhes();
            }
        });
        javax.swing.event.DocumentListener filtroListener = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                aplicarFiltros();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                aplicarFiltros();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                aplicarFiltros();
            }
        };
        campoDataInicial.getDocument().addDocumentListener(filtroListener);
        campoDataFinal.getDocument().addDocumentListener(filtroListener);
        campoProduto.getDocument().addDocumentListener(filtroListener);
        campoCliente.getDocument().addDocumentListener(filtroListener);
        btnLimparFiltros.addActionListener(event -> limparFiltros());
    }

    public void atualizarVendas(List<Venda> vendas) {
        this.vendas = List.copyOf(vendas);
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        LocalDate dataInicial;
        LocalDate dataFinal;
        try {
            dataInicial = parseData(campoDataInicial.getText());
            dataFinal = parseData(campoDataFinal.getText());
        } catch (DateTimeParseException erro) {
            exibirEstadoFiltroInvalido("Data inválida. Use o formato dd/MM/aaaa.");
            return;
        }
        if (dataInicial != null && dataFinal != null && dataFinal.isBefore(dataInicial)) {
            exibirEstadoFiltroInvalido("A data final deve ser igual ou posterior à data inicial.");
            return;
        }

        String termoProduto = campoProduto.getText().trim().toLowerCase(Locale.ROOT);
        String termoCliente = campoCliente.getText().trim().toLowerCase(Locale.ROOT);
        vendasExibidas = vendas.stream()
                .filter(venda -> dataInicial == null || !venda.getDataHora().toLocalDate().isBefore(dataInicial))
                .filter(venda -> dataFinal == null || !venda.getDataHora().toLocalDate().isAfter(dataFinal))
                .filter(venda -> termoCliente.isEmpty()
                        || nomeCliente(venda).toLowerCase(Locale.ROOT).contains(termoCliente))
                .filter(venda -> termoProduto.isEmpty()
                        || venda.getItens().stream().anyMatch(item ->
                                item.getNomeProduto().toLowerCase(Locale.ROOT).contains(termoProduto)))
                .collect(Collectors.toCollection(ArrayList::new));

        tabelaVendas.clearSelection();
        tableModel.setRowCount(0);
        areaDetalhes.setText("");
        for (Venda venda : vendasExibidas) {
            tableModel.addRow(new Object[] {
                    venda.getId(),
                    venda.getDataHora().format(DATA_HORA_FORMATTER),
                    nomeCliente(venda),
                    MOEDA_FORMATTER.format(venda.getValorTotal())
            });
        }
        lblStatus.setText(String.format("%d de %d venda(s)", vendasExibidas.size(), vendas.size()));
    }

    private LocalDate parseData(String texto) {
        String valor = texto.trim();
        return valor.isEmpty() ? null : LocalDate.parse(valor, DATA_FILTRO_FORMATTER);
    }

    private String nomeCliente(Venda venda) {
        return venda.getCliente() == null ? "Consumidor não informado" : venda.getCliente().getNome();
    }

    private void exibirEstadoFiltroInvalido(String mensagem) {
        vendasExibidas = List.of();
        tabelaVendas.clearSelection();
        tableModel.setRowCount(0);
        areaDetalhes.setText("");
        lblStatus.setText(mensagem);
    }

    private void limparFiltros() {
        campoDataInicial.setText("");
        campoDataFinal.setText("");
        campoProduto.setText("");
        campoCliente.setText("");
    }

    public JTextField getCampoDataInicial() {
        return campoDataInicial;
    }

    public JTextField getCampoDataFinal() {
        return campoDataFinal;
    }

    public JTextField getCampoProduto() {
        return campoProduto;
    }

    public JTextField getCampoCliente() {
        return campoCliente;
    }

    public JButton getBtnLimparFiltros() {
        return btnLimparFiltros;
    }

    public String getStatusTexto() {
        return lblStatus.getText();
    }

    public void exibirErro(String mensagem) {
        lblStatus.setText("Falha ao carregar o histórico: " + mensagem);
    }

    private void atualizarDetalhes() {
        int linhaSelecionada = tabelaVendas.getSelectedRow();
        if (linhaSelecionada < 0 || linhaSelecionada >= vendasExibidas.size()) {
            areaDetalhes.setText("");
            return;
        }

        Venda venda = vendasExibidas.get(linhaSelecionada);
        StringBuilder detalhes = new StringBuilder();
        for (ItemVenda item : venda.getItens()) {
            if (detalhes.length() > 0) {
                detalhes.append(System.lineSeparator());
            }
            detalhes.append(item.getNomeProduto())
                    .append(" — ")
                    .append(item.getQuantidade())
                    .append(" × ")
                    .append(MOEDA_FORMATTER.format(item.getPrecoUnitario()))
                    .append(" = ")
                    .append(MOEDA_FORMATTER.format(item.getSubtotal()));
        }
        detalhes.append(System.lineSeparator())
                .append("Total da venda: ")
                .append(MOEDA_FORMATTER.format(venda.getValorTotal()));
        areaDetalhes.setText(detalhes.toString());
        areaDetalhes.setCaretPosition(0);
    }

    public JButton getBtnAtualizar() {
        return btnAtualizar;
    }

    public JButton getBtnVoltar() {
        return btnVoltar;
    }

    public JTable getTabelaVendas() {
        return tabelaVendas;
    }

    public JTextArea getAreaDetalhes() {
        return areaDetalhes;
    }
}
