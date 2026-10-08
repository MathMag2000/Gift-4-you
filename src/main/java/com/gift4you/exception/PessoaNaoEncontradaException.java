package com.gift4you.exception;

public class PessoaNaoEncontradaException extends RegistroNaoEncontradoException {

    public PessoaNaoEncontradaException(int id) {
        super("Pessoa com id " + id + " não encontrada.");
    }
}
