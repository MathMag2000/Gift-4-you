package com.gift4you.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro de uma geração de ideias, com a ocasião e o orçamento usados naquele momento.
 * Pertence à conta e guarda o nome da pessoa, para continuar disponível mesmo se ela for removida:
 * nesse caso o registro perde o vínculo com a pessoa ({@link #isPessoaRemovida()}).
 */
public class RegistroHistorico implements Identificavel {

    private Integer id;
    private final int usuarioId;
    private Integer pessoaId;
    private final String pessoaNome;
    private final OrigemSugestao origem;
    private final Ocasiao ocasiao;
    private final FaixaOrcamento orcamento;
    private final List<ItemSugerido> itens;
    private final LocalDateTime realizadoEm;

    public RegistroHistorico(int usuarioId, Integer pessoaId, String pessoaNome, OrigemSugestao origem,
                             Ocasiao ocasiao, FaixaOrcamento orcamento, List<ItemSugerido> itens,
                             LocalDateTime realizadoEm) {
        this.usuarioId = usuarioId;
        this.pessoaId = pessoaId;
        this.pessoaNome = pessoaNome;
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

    public int getUsuarioId() {
        return usuarioId;
    }

    public boolean pertenceA(int idUsuario) {
        return usuarioId == idUsuario;
    }

    /** Nulo quando a pessoa foi removida. */
    public Integer getPessoaId() {
        return pessoaId;
    }

    public boolean eDaPessoa(int idPessoa) {
        return pessoaId != null && pessoaId == idPessoa;
    }

    /** Nome da pessoa quando as ideias foram geradas. */
    public String getPessoaNome() {
        return pessoaNome;
    }

    public boolean isPessoaRemovida() {
        return pessoaId == null;
    }

    public void desvincularPessoa() {
        this.pessoaId = null;
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
