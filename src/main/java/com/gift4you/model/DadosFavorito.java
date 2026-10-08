package com.gift4you.model;

/**
 * Pedido para favoritar um item. Para itens do catálogo basta o {@code presenteId} do item:
 * os demais dados são lidos do catálogo.
 */
public record DadosFavorito(Integer pessoaId, OrigemSugestao origem, ItemSugerido item) {
}
