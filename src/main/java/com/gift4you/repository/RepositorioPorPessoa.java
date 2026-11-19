package com.gift4you.repository;

import com.gift4you.model.Identificavel;
import com.gift4you.model.PertencePessoa;

import java.util.Collection;
import java.util.List;

public interface RepositorioPorPessoa<T extends Identificavel & PertencePessoa> extends Repositorio<T> {

    List<T> listarPorPessoa(int pessoaId);

    /** Usado para listar os dados de todas as pessoas de um usuário. */
    List<T> listarPorPessoas(Collection<Integer> pessoaIds);

    void removerPorPessoa(int pessoaId);
}
