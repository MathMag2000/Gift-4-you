package com.gift4you.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param linkCompra página da loja onde o presente é vendido; a imagem é obtida dela
 */
public record DadosPresente(
        String nome,
        Categoria categoria,
        String descricao,
        BigDecimal preco,
        List<String> caracteristicas,
        List<Ocasiao> ocasioes,
        String linkCompra) {
}
