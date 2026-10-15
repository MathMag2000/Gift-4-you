package com.gift4you.service;

import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Pessoa;

import java.util.List;

/**
 * Fonte externa de ideias de presentes (ex.: um modelo de IA).
 */
public interface GeradorIdeias {

    List<IdeiaPresente> gerarIdeias(Pessoa pessoa, int quantidade);
}
