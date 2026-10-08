package com.gift4you.exception;

public class FavoritoNaoEncontradoException extends RegistroNaoEncontradoException {

    public FavoritoNaoEncontradoException(int id) {
        super("Favorito com id " + id + " não encontrado.");
    }
}
