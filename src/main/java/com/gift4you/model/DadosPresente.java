package com.gift4you.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param linkCompra página da loja onde o presente é vendido; a imagem é obtida dela
 * @param pessoaId   pessoa para quem o presente foi cadastrado; nulo quando é um presente geral do catálogo
 */
public record DadosPresente(
        String nome,
        Categoria categoria,
        String descricao,
        BigDecimal preco,
        List<String> caracteristicas,
        List<Ocasiao> ocasioes,
        String linkCompra,
        Integer pessoaId) {

    /** Presente geral do catálogo, sem pessoa definida. */
    public DadosPresente(String nome, Categoria categoria, String descricao, BigDecimal preco,
                         List<String> caracteristicas, List<Ocasiao> ocasioes, String linkCompra) {
        this(nome, categoria, descricao, preco, caracteristicas, ocasioes, linkCompra, null);
    }
}
