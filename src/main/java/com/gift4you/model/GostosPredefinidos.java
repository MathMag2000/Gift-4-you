package com.gift4you.model;

import java.util.List;

/**
 * Gostos e interesses oferecidos como tags no cadastro de pessoas. A pessoa também pode ter gostos
 * digitados livremente; por isso os gostos são guardados como texto, e não como enum.
 * As tags ficam em minúsculas, o mesmo formato em que os gostos são guardados.
 */
public final class GostosPredefinidos {

    public static final List<String> TAGS = List.of(
            "jogos", "música", "roupas", "livros", "filmes", "séries", "esportes", "tecnologia",
            "cozinha", "café", "viagem", "arte", "fotografia", "beleza", "decoração", "plantas",
            "pets", "chocolate", "fitness", "acessórios");

    private GostosPredefinidos() {
    }
}
