package com.gift4you.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cópia dos dados de um presente sugerido, usada no histórico e nos favoritos.
 * Guardar a cópia mantém o registro mesmo se o presente do catálogo mudar, e permite
 * guardar ideias da IA, que não existem no catálogo.
 *
 * @param presenteId id no catálogo; nulo para ideias da IA
 * @param linkCompra página da loja; para ideias da IA, a busca do produto numa loja
 * @param imagemUrl  imagem obtida da página da loja; nula se não houver
 */
public record ItemSugerido(Integer presenteId, String nome, String categoria, BigDecimal preco,
                           List<String> motivos, String linkCompra, String imagemUrl) {

    public static ItemSugerido de(Presente presente, List<String> motivos) {
        return new ItemSugerido(presente.getId(), presente.getNome(), presente.getCategoria().toString(),
                presente.getPreco(), List.copyOf(motivos), presente.getLinkCompra(), presente.getImagemUrl());
    }

    public static ItemSugerido de(SugestaoPresente sugestao) {
        return de(sugestao.presente(), sugestao.motivos());
    }

    public static ItemSugerido de(IdeiaPresente ideia) {
        List<String> motivos = ideia.motivo().isBlank() ? List.of() : List.of(ideia.motivo());
        return new ItemSugerido(null, ideia.nome(), ideia.categoria(), ideia.precoEstimado(), motivos,
                ideia.linkCompra(), null);
    }

    public ItemSugerido comLinkCompra(String link) {
        return new ItemSugerido(presenteId, nome, categoria, preco, motivos, link, imagemUrl);
    }
}
