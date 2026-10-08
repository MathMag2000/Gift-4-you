package com.gift4you.service;

/**
 * Recebe o aviso de que uma pessoa foi removida, para apagar os dados ligados a ela.
 */
public interface OuvinteRemocaoPessoa {

    void aoRemoverPessoa(int pessoaId);
}
