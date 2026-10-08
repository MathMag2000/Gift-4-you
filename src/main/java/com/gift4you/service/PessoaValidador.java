package com.gift4you.service;

import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.DadosPessoa;
import com.gift4you.model.FaixaOrcamento;

import java.util.List;

public class PessoaValidador extends ValidadorBase {

    private static final int IDADE_MINIMA = 0;
    private static final int IDADE_MAXIMA = 120;

    /**
     * Valida os dados informados e devolve uma cópia normalizada
     * (nome sem espaços nas pontas e listas em minúsculas, sem itens vazios ou repetidos).
     */
    public DadosPessoa validar(DadosPessoa dados) {
        String nome = exigirTexto(dados.nome(), "O nome é obrigatório.");
        List<String> gostos = normalizarLista(dados.gostos());
        List<String> interesses = normalizarLista(dados.interesses());
        List<String> naoGosta = normalizarLista(dados.naoGosta());

        validarIdade(dados.idade());
        exigir(dados.vinculo(), "O vínculo é obrigatório.");
        exigir(dados.ocasiao(), "A ocasião é obrigatória.");
        validarOrcamento(dados.orcamento());
        validarSemConflito(gostos, naoGosta, "gostos");
        validarSemConflito(interesses, naoGosta, "interesses");

        return new DadosPessoa(nome, dados.idade(), dados.vinculo(),
                gostos, interesses, naoGosta, dados.ocasiao(), dados.orcamento());
    }

    private void validarIdade(Integer idade) {
        exigir(idade, "A idade é obrigatória.");
        if (idade < IDADE_MINIMA || idade > IDADE_MAXIMA) {
            throw new ValidacaoException(
                    "A idade deve estar entre " + IDADE_MINIMA + " e " + IDADE_MAXIMA + " anos.");
        }
    }

    private void validarOrcamento(FaixaOrcamento orcamento) {
        exigir(orcamento, "A faixa de orçamento é obrigatória.");
        exigir(orcamento.maximo(), "O orçamento máximo é obrigatório.");
        if (orcamento.maximo().signum() <= 0) {
            throw new ValidacaoException("O orçamento máximo deve ser maior que zero.");
        }
    }

    private void validarSemConflito(List<String> preferidos, List<String> naoGosta, String nomeLista) {
        List<String> conflitos = preferidos.stream().filter(naoGosta::contains).toList();
        if (!conflitos.isEmpty()) {
            throw new ValidacaoException("Itens presentes em " + nomeLista
                    + " e em \"não gosta\" ao mesmo tempo: " + String.join(", ", conflitos));
        }
    }
}
