package com.gift4you.repository;

import com.gift4you.model.Usuario;

import java.util.Optional;

public class UsuarioRepositoryEmMemoria extends RepositorioEmMemoria<Usuario> implements UsuarioRepository {

    @Override
    public synchronized Optional<Usuario> buscarPorEmail(String email) {
        return listarTodos().stream().filter(usuario -> usuario.getEmail().equals(email)).findFirst();
    }
}
