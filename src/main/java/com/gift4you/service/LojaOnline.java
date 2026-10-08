package com.gift4you.service;

import com.gift4you.model.ProdutoLoja;

import java.util.Optional;

/**
 * Loja onde as ideias da IA são procuradas, para levar o usuário à página do produto.
 */
public interface LojaOnline {

    /** Página de busca do produto na loja, usada quando o produto específico não é encontrado. */
    String linkBusca(String nomeProduto);

    /**
     * Procura o produto pelo nome e devolve o primeiro resultado que não é anúncio.
     *
     * @return vazio se nada for encontrado ou se a loja não puder ser consultada
     */
    Optional<ProdutoLoja> encontrarProduto(String nomeProduto);

    /** Indica se o link é a página de um produto desta loja, para aceitar links vindos do navegador. */
    boolean ehLinkDeProduto(String link);

    /** Indica se a imagem vem do servidor de imagens desta loja. */
    boolean ehImagemDaLoja(String imagemUrl);
}
