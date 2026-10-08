package com.gift4you.model;

import java.math.BigDecimal;

/**
 * Ideia de presente gerada por IA. Diferente de {@link Presente}, não faz parte do catálogo.
 */
public record IdeiaPresente(String nome, String categoria, BigDecimal precoEstimado, String motivo) {
}
