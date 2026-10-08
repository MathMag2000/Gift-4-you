package com.gift4you.model;

import java.math.BigDecimal;

/**
 * Ideia de presente gerada por IA. Diferente de {@link Presente}, não faz parte do catálogo.
 *
 * @param linkCompra link de busca do produto numa loja; nulo até o sistema defini-lo
 */
public record IdeiaPresente(String nome, String categoria, BigDecimal precoEstimado, String motivo,
                            String linkCompra) {

    /** Ideia como chega da IA, ainda sem link de compra. */
    public IdeiaPresente(String nome, String categoria, BigDecimal precoEstimado, String motivo) {
        this(nome, categoria, precoEstimado, motivo, null);
    }

    public IdeiaPresente comLinkCompra(String link) {
        return new IdeiaPresente(nome, categoria, precoEstimado, motivo, link);
    }
}
