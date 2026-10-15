package com.gift4you.model;

public enum Vinculo {

    PAI("Pai"),
    MAE("Mãe"),
    FILHO("Filho(a)"),
    IRMAO("Irmão(ã)"),
    AVO("Avô(ó)"),
    PARENTE("Outro parente"),
    CONJUGE("Cônjuge"),
    NAMORADO("Namorado(a)"),
    AMIGO("Amigo(a)"),
    COLEGA_DE_TRABALHO("Colega de trabalho"),
    OUTRO("Outro");

    private final String descricao;

    Vinculo(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
