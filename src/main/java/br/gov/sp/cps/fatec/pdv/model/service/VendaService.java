package br.gov.sp.cps.fatec.pdv.model.service;

import br.gov.sp.cps.fatec.pdv.model.entity.Venda;
import br.gov.sp.cps.fatec.pdv.model.repository.ItemVendaSolicitado;
import br.gov.sp.cps.fatec.pdv.model.repository.VendaRepository;

import java.util.List;
import java.util.Objects;

public class VendaService {

    private final VendaRepository repository;

    public VendaService(VendaRepository repository) {
        this.repository = Objects.requireNonNull(repository, "VendaRepository não pode ser nulo.");
    }

    public Venda registrar(String nomeCliente, List<ItemVendaSolicitado> itens) {
        return repository.registrar(nomeCliente, itens);
    }

    public List<Venda> listarTodas() {
        return repository.listarTodas();
    }
}
