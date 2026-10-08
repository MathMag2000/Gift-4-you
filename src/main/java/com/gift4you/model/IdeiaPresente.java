package com.gift4you.model;

import java.math.BigDecimal;

/**
 * Ideia de presente gerada por IA. Diferente de {@link Presente}, não faz parte do catálogo.
 *
 * @param precoEstimado preço estimado pela IA; quando o produto é encontrado na loja, o preço dele
 * @param linkCompra    página do produto encontrado na loja, ou a busca dele; nulo até o sistema defini-lo
 * @param imagemUrl     foto do produto encontrado na loja; nula se ele não foi encontrado
 */
public record IdeiaPresente(String nome, String categoria, BigDecimal precoEstimado, String motivo,
                            String linkCompra, String imagemUrl) {

    /** Ideia como chega da IA, ainda sem link de compra. */
    public IdeiaPresente(String nome, String categoria, BigDecimal precoEstimado, String motivo) {
        this(nome, categoria, precoEstimado, motivo, null, null);
    }

    /** Troca o nome e o preço sugeridos pela IA pelos do produto, para que correspondam ao link. */
    public IdeiaPresente comProduto(ProdutoLoja produto) {
        String nomeProduto = produto.titulo().isBlank() ? nome : produto.titulo();
        return new IdeiaPresente(nomeProduto, categoria, produto.preco(), motivo, produto.link(), produto.imagemUrl());
    }

    public IdeiaPresente comLinkBusca(String linkBusca) {
        return new IdeiaPresente(nome, categoria, precoEstimado, motivo, linkBusca, null);
    }
}
