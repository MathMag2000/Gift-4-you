package com.gift4you.model;

import java.math.BigDecimal;

/** Orçamento da pessoa: o presente pode custar qualquer valor até o máximo. */
public record FaixaOrcamento(BigDecimal maximo) {
}
