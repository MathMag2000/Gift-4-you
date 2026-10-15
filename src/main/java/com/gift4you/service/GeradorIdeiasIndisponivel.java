package com.gift4you.service;

import com.gift4you.exception.IntegracaoIaException;
import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Pessoa;

import java.util.List;

/**
 * Usado quando nenhuma IA está configurada: o restante do sistema funciona normalmente.
 */
public class GeradorIdeiasIndisponivel implements GeradorIdeias {

    private final String motivo;

    public GeradorIdeiasIndisponivel(String motivo) {
        this.motivo = motivo;
    }

    @Override
    public List<IdeiaPresente> gerarIdeias(Pessoa pessoa, int quantidade) {
        throw new IntegracaoIaException(motivo);
    }
}
