package com.gift4you.model;

import java.util.List;

/**
 * @param gostos      gostos e interesses da pessoa, vindos das tags predefinidas ou digitados
 * @param estilos     estilo de presente preferido (ex.: prático, criativo), das tags de {@link EstilosPredefinidos}
 * @param observacoes informações adicionais em texto livre, usadas pela IA
 */
public record DadosPessoa(
        String nome,
        Integer idade,
        Vinculo vinculo,
        List<String> gostos,
        List<String> naoGosta,
        List<String> estilos,
        Ocasiao ocasiao,
        FaixaOrcamento orcamento,
        String observacoes) {

    /** Pessoa sem estilo nem observações, campos opcionais. */
    public DadosPessoa(String nome, Integer idade, Vinculo vinculo, List<String> gostos, List<String> naoGosta,
                       Ocasiao ocasiao, FaixaOrcamento orcamento) {
        this(nome, idade, vinculo, gostos, naoGosta, List.of(), ocasiao, orcamento, "");
    }
}
