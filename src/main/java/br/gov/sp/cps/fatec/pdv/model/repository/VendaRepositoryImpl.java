package br.gov.sp.cps.fatec.pdv.model.repository;

import br.gov.sp.cps.fatec.pdv.model.entity.Cliente;
import br.gov.sp.cps.fatec.pdv.model.entity.ItemVenda;
import br.gov.sp.cps.fatec.pdv.model.entity.Produto;
import br.gov.sp.cps.fatec.pdv.model.entity.Venda;
import br.gov.sp.cps.fatec.pdv.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VendaRepositoryImpl implements VendaRepository {

    private final EntityManagerFactory emf;

    public VendaRepositoryImpl() {
        this(JPAUtil.getEntityManagerFactory());
    }

    public VendaRepositoryImpl(EntityManagerFactory emf) {
        this.emf = Objects.requireNonNull(emf, "EntityManagerFactory não pode ser nulo.");
    }

    @Override
    public Venda registrar(String nomeCliente, List<ItemVendaSolicitado> itensSolicitados) {
        Map<String, Integer> quantidadesPorCodigo = validarEConsolidarItens(itensSolicitados);
        String nomeNormalizado = normalizarNomeCliente(nomeCliente);

        EntityManager em = emf.createEntityManager();
        EntityTransaction transacao = em.getTransaction();
        try {
            transacao.begin();

            Cliente cliente = null;
            if (nomeNormalizado != null) {
                cliente = new Cliente(nomeNormalizado);
                em.persist(cliente);
            }

            Venda venda = new Venda(LocalDateTime.now(), cliente);
            for (Map.Entry<String, Integer> itemSolicitado : quantidadesPorCodigo.entrySet()) {
                TypedQuery<Produto> consulta = em.createQuery(
                        "SELECT p FROM Produto p WHERE p.codigoBarras = :codigo",
                        Produto.class);
                consulta.setParameter("codigo", itemSolicitado.getKey());
                consulta.setLockMode(LockModeType.PESSIMISTIC_WRITE);
                List<Produto> resultados = consulta.getResultList();
                if (resultados.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Produto não encontrado para o código de barras: " + itemSolicitado.getKey());
                }

                Produto produto = resultados.get(0);
                int quantidade = itemSolicitado.getValue();
                if (produto.getQuantidadeEstoque() < quantidade) {
                    throw new IllegalArgumentException(
                            "Estoque insuficiente para '" + produto.getNome() + "'. Disponível: "
                                    + produto.getQuantidadeEstoque() + ", solicitado: " + quantidade + ".");
                }

                produto.setQuantidadeEstoque(produto.getQuantidadeEstoque() - quantidade);
                venda.adicionarItem(new ItemVenda(venda, produto, quantidade));
            }

            em.persist(venda);
            transacao.commit();
            return venda;
        } catch (RuntimeException e) {
            if (transacao.isActive()) {
                transacao.rollback();
            }
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalStateException("Falha ao registrar venda; nenhuma alteração foi confirmada.", e);
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    @Override
    public List<Venda> listarTodas() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery(
                    "SELECT DISTINCT v FROM Venda v "
                            + "LEFT JOIN FETCH v.cliente "
                            + "LEFT JOIN FETCH v.itens i "
                            + "LEFT JOIN FETCH i.produto "
                            + "ORDER BY v.dataHora DESC, v.id DESC",
                    Venda.class).getResultList();
        } finally {
            if (em.isOpen()) {
                em.close();
            }
        }
    }

    private Map<String, Integer> validarEConsolidarItens(List<ItemVendaSolicitado> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new IllegalArgumentException("Adicione pelo menos um produto à venda.");
        }

        Map<String, Integer> quantidades = new LinkedHashMap<>();
        for (ItemVendaSolicitado item : itens) {
            if (item == null || item.codigoBarras() == null || item.codigoBarras().trim().isEmpty()) {
                throw new IllegalArgumentException("Cada item da venda deve ter um código de barras.");
            }
            if (item.quantidade() <= 0) {
                throw new IllegalArgumentException("A quantidade de cada item deve ser maior que zero.");
            }
            String codigo = item.codigoBarras().trim();
            try {
                quantidades.merge(codigo, item.quantidade(), Math::addExact);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("A quantidade total de um produto excede o limite permitido.", e);
            }
        }
        return quantidades;
    }

    private String normalizarNomeCliente(String nomeCliente) {
        if (nomeCliente == null || nomeCliente.trim().isEmpty()) {
            return null;
        }
        String nome = nomeCliente.trim();
        if (nome.length() > 120) {
            throw new IllegalArgumentException("O nome do cliente deve ter no máximo 120 caracteres.");
        }
        return nome;
    }
}
