package com.gift4you.exception;

public class IntegracaoIaException extends RuntimeException {

    public IntegracaoIaException(String mensagem) {
        super(mensagem);
    }

    public IntegracaoIaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
