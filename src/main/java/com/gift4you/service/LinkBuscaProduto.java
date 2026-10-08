package com.gift4you.service;

/**
 * Monta o link de busca de um produto numa loja, usado nas ideias da IA, que não têm página de produto conhecida.
 */
public interface LinkBuscaProduto {

    String linkPara(String nomeProduto);
}
