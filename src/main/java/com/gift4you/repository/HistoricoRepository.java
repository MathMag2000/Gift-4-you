package com.gift4you.repository;

import com.gift4you.model.RegistroHistorico;

import java.util.List;

public interface HistoricoRepository extends Repositorio<RegistroHistorico> {

    List<RegistroHistorico> listarPorUsuario(int usuarioId);

    List<RegistroHistorico> listarPorPessoa(int pessoaId);

    /** Mantém os registros da pessoa removida, sem o vínculo com ela. */
    void desvincularPessoa(int pessoaId);
}
