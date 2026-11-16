package com.gift4you.repository;

import com.gift4you.model.Pessoa;

import java.util.List;

public class PessoaRepositoryEmMemoria extends RepositorioEmMemoria<Pessoa> implements PessoaRepository {

    @Override
    public synchronized List<Pessoa> listarPorUsuario(int usuarioId) {
        return listarTodos().stream().filter(pessoa -> pessoa.pertenceA(usuarioId)).toList();
    }
}
