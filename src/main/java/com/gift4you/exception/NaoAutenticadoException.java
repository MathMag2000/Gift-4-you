package com.gift4you.exception;

/** O pedido exige um usuário logado, e não há nenhum na sessão. */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException() {
        this("Faça login para continuar.");
    }

    protected NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
