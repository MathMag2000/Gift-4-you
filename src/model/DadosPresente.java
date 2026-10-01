package model;

import java.math.BigDecimal;
import java.util.List;

public record DadosPresente(
        String nome,
        Categoria categoria,
        String descricao,
        BigDecimal preco,
        List<String> caracteristicas,
        List<Ocasiao> ocasioes) {
}
