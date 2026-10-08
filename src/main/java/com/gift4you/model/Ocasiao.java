package com.gift4you.model;

public enum Ocasiao {

    ANIVERSARIO("Aniversário"),
    NATAL("Natal"),
    DIA_DAS_MAES("Dia das Mães"),
    DIA_DOS_PAIS("Dia dos Pais"),
    DIA_DOS_NAMORADOS("Dia dos Namorados"),
    CASAMENTO("Casamento"),
    FORMATURA("Formatura"),
    AMIGO_SECRETO("Amigo secreto"),
    OUTRA("Outra");

    private final String descricao;

    Ocasiao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
