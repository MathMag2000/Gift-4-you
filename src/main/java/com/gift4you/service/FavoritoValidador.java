package com.gift4you.service;

import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.DadosFavorito;
import com.gift4you.model.ItemSugerido;

import java.util.List;
import java.util.Objects;

public class FavoritoValidador extends ValidadorBase {

    public void validarPedido(DadosFavorito dados) {
        exigir(dados.pessoaId(), "Informe a pessoa do favorito.");
        exigir(dados.origem(), "Informe a origem do favorito.");
        exigir(dados.item(), "Informe o item a favoritar.");
    }

    public int exigirPresenteId(ItemSugerido item) {
        exigir(item.presenteId(), "Informe o presente do catálogo a favoritar.");
        return item.presenteId();
    }

    /**
     * Valida uma ideia da IA, que só existe nos dados enviados, e devolve uma cópia normalizada.
     * Link e imagem enviados são descartados: ideias da IA não vêm de uma loja.
     */
    public ItemSugerido validarIdeia(ItemSugerido item) {
        String nome = exigirTexto(item.nome(), "O nome da ideia é obrigatório.");
        exigir(item.preco(), "O preço da ideia é obrigatório.");
        if (item.preco().signum() <= 0) {
            throw new ValidacaoException("O preço da ideia deve ser maior que zero.");
        }
        String categoria = item.categoria() == null ? "" : item.categoria().trim();
        List<String> motivos = item.motivos() == null ? List.of()
                : item.motivos().stream().filter(Objects::nonNull).map(String::trim).filter(m -> !m.isEmpty()).toList();
        return new ItemSugerido(null, nome, categoria, item.preco(), motivos, null, null);
    }
}
