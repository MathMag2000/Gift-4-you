package com.gift4you.model;

public enum OrigemSugestao {

    CATALOGO("Catálogo"),
    IA("IA");

    private final String descricao;

    OrigemSugestao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
