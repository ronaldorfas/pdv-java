package br.gov.sp.cps.fatec.pdv.model.repository;

import br.gov.sp.cps.fatec.pdv.model.entity.Venda;

import java.util.List;

public interface VendaRepository {

    Venda registrar(String nomeCliente, List<ItemVendaSolicitado> itens);

    List<Venda> listarTodas();
}
