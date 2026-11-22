package com.gift4you.model;

import java.util.List;

/**
 * Ideias aprovadas pelas regras de orçamento e rejeições, quantas a IA sugeriu fora dessas regras e
 * quantas repetiam ideias já sugeridas antes para a mesma pessoa.
 */
public record ResultadoIdeias(List<IdeiaPresente> ideias, int descartadas, int repetidas) {

    public ResultadoIdeias(List<IdeiaPresente> ideias, int descartadas) {
        this(ideias, descartadas, 0);
    }
}
