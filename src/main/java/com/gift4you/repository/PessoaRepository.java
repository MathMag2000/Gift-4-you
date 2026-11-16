package com.gift4you.repository;

import com.gift4you.model.Pessoa;

import java.util.List;

public interface PessoaRepository extends Repositorio<Pessoa> {

    List<Pessoa> listarPorUsuario(int usuarioId);
}
