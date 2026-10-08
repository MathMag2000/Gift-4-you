package com.gift4you.repository;

import com.gift4you.model.Identificavel;
import com.gift4you.model.PertencePessoa;

import java.util.List;

public abstract class RepositorioPorPessoaEmMemoria<T extends Identificavel & PertencePessoa>
        extends RepositorioEmMemoria<T> implements RepositorioPorPessoa<T> {

    @Override
    public synchronized List<T> listarPorPessoa(int pessoaId) {
        return listarTodos().stream().filter(entidade -> entidade.getPessoaId() == pessoaId).toList();
    }

    @Override
    public synchronized void removerPorPessoa(int pessoaId) {
        listarPorPessoa(pessoaId).forEach(entidade -> remover(entidade.getId()));
    }
}
