package com.gift4you.model;

import java.math.BigDecimal;

/**
 * Produto encontrado numa loja online a partir do nome de uma ideia de presente.
 *
 * @param link      página do produto na loja
 * @param imagemUrl foto do produto; pode ser nula
 * @param preco     preço atual na loja; nulo se a página não o informar
 */
public record ProdutoLoja(String titulo, String link, String imagemUrl, BigDecimal preco) {
}
