package com.gift4you.integracao;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

/**
 * Requisições que se identificam como um navegador comum, pois muitas lojas recusam clientes desconhecidos.
 */
final class RequisicaoNavegador {

    private static final String NAVEGADOR = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/130.0 Safari/537.36";

    private RequisicaoNavegador() {
    }

    static HttpRequest paginaHtml(URI endereco, Duration tempoLimite) {
        return HttpRequest.newBuilder(endereco)
                .timeout(tempoLimite)
                .header("User-Agent", NAVEGADOR)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .GET()
                .build();
    }
}
