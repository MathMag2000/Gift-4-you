package com.gift4you.exception;

public class RegistroHistoricoNaoEncontradoException extends RegistroNaoEncontradoException {

    public RegistroHistoricoNaoEncontradoException(int id) {
        super("Registro de histórico com id " + id + " não encontrado.");
    }
}
