package com.gift4you.exception;

public class UsuarioNaoEncontradoException extends RegistroNaoEncontradoException {

    public UsuarioNaoEncontradoException(int id) {
        super("Usuário com id " + id + " não encontrado.");
    }
}
