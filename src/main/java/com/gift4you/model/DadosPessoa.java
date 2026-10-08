package com.gift4you.model;

import java.util.List;

public record DadosPessoa(
        String nome,
        Integer idade,
        Vinculo vinculo,
        List<String> gostos,
        List<String> interesses,
        List<String> naoGosta,
        Ocasiao ocasiao,
        FaixaOrcamento orcamento) {
}
