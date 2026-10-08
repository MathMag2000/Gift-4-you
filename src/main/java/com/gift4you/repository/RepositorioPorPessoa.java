package com.gift4you.repository;

import com.gift4you.model.Identificavel;
import com.gift4you.model.PertencePessoa;

import java.util.List;

public interface RepositorioPorPessoa<T extends Identificavel & PertencePessoa> extends Repositorio<T> {

    List<T> listarPorPessoa(int pessoaId);

    void removerPorPessoa(int pessoaId);
}
