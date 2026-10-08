package com.gift4you.service;

import com.gift4you.model.ProdutoLoja;

import java.util.List;

/**
 * Loja onde as ideias da IA são procuradas, para levar o usuário à página do produto.
 */
public interface LojaOnline {

    /** Página de busca do produto na loja, usada quando o produto específico não é encontrado. */
    String linkBusca(String nomeProduto);

    /**
     * Procura o produto pelo nome e devolve os resultados que não são anúncios, na ordem da busca.
     *
     * @return vazia se nada for encontrado ou se a loja não puder ser consultada
     */
    List<ProdutoLoja> encontrarProdutos(String nomeProduto);

    /** Indica se o link é a página de um produto desta loja, para aceitar links vindos do navegador. */
    boolean ehLinkDeProduto(String link);

    /** Indica se a imagem vem do servidor de imagens desta loja. */
    boolean ehImagemDaLoja(String imagemUrl);
}
