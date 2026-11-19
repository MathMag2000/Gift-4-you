package com.gift4you.exception;

/**
 * E-mail ou senha incorretos. A mensagem é a mesma nos dois casos para não revelar
 * quais e-mails têm conta no sistema.
 */
public class CredenciaisInvalidasException extends NaoAutenticadoException {

    public CredenciaisInvalidasException() {
        super("E-mail ou senha incorretos.");
    }
}
