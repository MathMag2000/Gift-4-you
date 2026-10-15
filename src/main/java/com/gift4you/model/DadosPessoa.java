package com.gift4you.model;

import java.util.List;

/**
 * @param gostos gostos e interesses da pessoa, vindos das tags predefinidas ou digitados
 */
public record DadosPessoa(
        String nome,
        Integer idade,
        Vinculo vinculo,
        List<String> gostos,
        List<String> naoGosta,
        Ocasiao ocasiao,
        FaixaOrcamento orcamento) {
}
