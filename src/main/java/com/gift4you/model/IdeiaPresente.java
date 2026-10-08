package com.gift4you.model;

import java.math.BigDecimal;

/**
 * Ideia de presente gerada por IA. Diferente de {@link Presente}, não faz parte do catálogo.
 *
 * @param linkCompra página do produto encontrado na loja, ou a busca dele; nulo até o sistema defini-lo
 * @param imagemUrl  foto do produto encontrado na loja; nula se ele não foi encontrado
 */
public record IdeiaPresente(String nome, String categoria, BigDecimal precoEstimado, String motivo,
                            String linkCompra, String imagemUrl) {

    /** Ideia como chega da IA, ainda sem link de compra. */
    public IdeiaPresente(String nome, String categoria, BigDecimal precoEstimado, String motivo) {
        this(nome, categoria, precoEstimado, motivo, null, null);
    }

    public IdeiaPresente naLoja(String link, String imagem) {
        return new IdeiaPresente(nome, categoria, precoEstimado, motivo, link, imagem);
    }
}
