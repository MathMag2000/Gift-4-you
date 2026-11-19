package com.gift4you.repository;

import com.gift4you.model.Presente;

import java.util.List;

public interface PresenteRepository extends Repositorio<Presente> {

    List<Presente> listarPorUsuario(int usuarioId);
}
