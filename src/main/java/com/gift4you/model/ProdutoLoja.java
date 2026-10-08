package com.gift4you.model;

/**
 * Produto encontrado numa loja online a partir do nome de uma ideia de presente.
 *
 * @param link      página do produto na loja
 * @param imagemUrl foto do produto; pode ser nula
 */
public record ProdutoLoja(String titulo, String link, String imagemUrl) {
}
