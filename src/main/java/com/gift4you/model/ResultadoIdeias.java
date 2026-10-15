package com.gift4you.model;

import java.util.List;

/**
 * Ideias aprovadas pelas regras de orçamento e rejeições, e quantas a IA sugeriu fora dessas regras.
 */
public record ResultadoIdeias(List<IdeiaPresente> ideias, int descartadas) {
}
