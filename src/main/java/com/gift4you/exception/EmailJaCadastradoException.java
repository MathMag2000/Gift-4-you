package com.gift4you.exception;

public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException() {
        super("Já existe uma conta com este e-mail.");
    }
}
