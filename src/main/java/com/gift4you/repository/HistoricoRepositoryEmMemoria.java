package com.gift4you.repository;

import com.gift4you.model.RegistroHistorico;

import java.util.List;

public class HistoricoRepositoryEmMemoria extends RepositorioEmMemoria<RegistroHistorico>
        implements HistoricoRepository {

    @Override
    public synchronized List<RegistroHistorico> listarPorUsuario(int usuarioId) {
        return listarTodos().stream().filter(registro -> registro.pertenceA(usuarioId)).toList();
    }

    @Override
    public synchronized List<RegistroHistorico> listarPorPessoa(int pessoaId) {
        return listarTodos().stream().filter(registro -> registro.eDaPessoa(pessoaId)).toList();
    }

    @Override
    public synchronized void desvincularPessoa(int pessoaId) {
        listarPorPessoa(pessoaId).forEach(RegistroHistorico::desvincularPessoa);
    }
}
