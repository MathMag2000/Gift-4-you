package com.gift4you.exception;

import java.time.Duration;

/** Muitos pedidos em pouco tempo (ex.: tentativas de login ou ideias com IA). Vira HTTP 429. */
public class LimiteExcedidoException extends RuntimeException {

    private final Duration esperar;

    public LimiteExcedidoException(String mensagem, Duration esperar) {
        super(mensagem);
        this.esperar = esperar;
    }

    /** Tempo até um novo pedido ser aceito. */
    public Duration getEsperar() {
        return esperar;
    }
}
