package com.gift4you.service;

import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Pessoa;

import java.util.List;

/**
 * Fonte externa de ideias de presentes (ex.: um modelo de IA).
 */
public interface GeradorIdeias {

    List<IdeiaPresente> gerarIdeias(Pessoa pessoa, int quantidade);

    /**
     * @param jaSugeridas nomes de ideias já sugeridas para a pessoa, que não devem ser repetidas. Fontes que
     *                    não sabem usar essa informação podem ignorá-la: o sistema descarta repetições depois.
     */
    default List<IdeiaPresente> gerarIdeias(Pessoa pessoa, int quantidade, List<String> jaSugeridas) {
        return gerarIdeias(pessoa, quantidade);
    }
}
