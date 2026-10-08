package com.gift4you.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de uma geração de sugestões, com a ocasião e o orçamento usados naquele momento.
 */
public class RegistroHistorico implements Identificavel, PertencePessoa {

    private Integer id;
    private final int pessoaId;
    private final OrigemSugestao origem;
    private final Ocasiao ocasiao;
    private final FaixaOrcamento orcamento;
    private final List<ItemSugerido> itens;
    private final LocalDateTime realizadoEm;

    public RegistroHistorico(int pessoaId, OrigemSugestao origem, Ocasiao ocasiao, FaixaOrcamento orcamento,
                             List<ItemSugerido> itens, LocalDateTime realizadoEm) {
        this.pessoaId = pessoaId;
        this.origem = origem;
        this.ocasiao = ocasiao;
        this.orcamento = orcamento;
        this.itens = List.copyOf(itens);
        this.realizadoEm = realizadoEm;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public int getPessoaId() {
        return pessoaId;
    }

    public OrigemSugestao getOrigem() {
        return origem;
    }

    public Ocasiao getOcasiao() {
        return ocasiao;
    }

    public FaixaOrcamento getOrcamento() {
        return orcamento;
    }

    public List<ItemSugerido> getItens() {
        return itens;
    }

    public LocalDateTime getRealizadoEm() {
        return realizadoEm;
    }
}
