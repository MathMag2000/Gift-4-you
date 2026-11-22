package com.gift4you.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cabeçalhos que pedem ao navegador proteções extras em todas as respostas do site.
 */
public class CabecalhosSegurancaFilter extends OncePerRequestFilter {

    /**
     * Content-Security-Policy: o site só executa os próprios scripts e estilos (mais as fontes do Google Fonts),
     * o que bloqueia scripts injetados. Imagens podem vir de qualquer loja em https, pois as fotos dos
     * produtos são das lojas. frame-ancestors 'none' impede que o site seja aberto dentro de outro.
     */
    static final String POLITICA_CONTEUDO = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' https: data:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    /** Um ano; só é enviado em conexões HTTPS, como pede a especificação. */
    private static final String HSTS = "max-age=31536000; includeSubDomains";

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        resposta.setHeader("Content-Security-Policy", POLITICA_CONTEUDO);
        resposta.setHeader("X-Content-Type-Options", "nosniff");
        resposta.setHeader("X-Frame-Options", "DENY");
        resposta.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        resposta.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()");
        if (requisicao.isSecure()) {
            resposta.setHeader("Strict-Transport-Security", HSTS);
        }
        cadeia.doFilter(requisicao, resposta);
    }
}
