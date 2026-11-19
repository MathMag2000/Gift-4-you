package com.gift4you.repository;

import com.gift4you.model.Usuario;

import java.util.Optional;

public interface UsuarioRepository extends Repositorio<Usuario> {

    /** @param email e-mail já normalizado (sem espaços nas pontas e em minúsculas) */
    Optional<Usuario> buscarPorEmail(String email);
}
