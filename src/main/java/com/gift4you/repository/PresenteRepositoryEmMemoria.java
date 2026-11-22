package com.gift4you.repository;

import com.gift4you.model.Presente;

import java.util.List;

public class PresenteRepositoryEmMemoria extends RepositorioEmMemoria<Presente> implements PresenteRepository {

    @Override
    public synchronized List<Presente> listarPorUsuario(int usuarioId) {
        return listarTodos().stream().filter(presente -> presente.pertenceA(usuarioId)).toList();
    }

    @Override
    public synchronized void desvincularPessoa(int pessoaId) {
        listarTodos().stream()
                .filter(presente -> presente.cadastradoPara(pessoaId))
                .forEach(Presente::desvincularPessoa);
    }
}
