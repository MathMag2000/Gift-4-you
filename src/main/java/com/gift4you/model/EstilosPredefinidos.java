package com.gift4you.model;

import java.util.List;

/**
 * Estilos de presente oferecidos como tags no cadastro de pessoas. Ficam em minúsculas, o mesmo formato
 * em que são guardados.
 */
public final class EstilosPredefinidos {

    public static final List<String> TAGS = List.of(
            "prático", "criativo", "luxuoso", "divertido", "minimalista", "personalizado", "útil", "experiência");

    private EstilosPredefinidos() {
    }
}
