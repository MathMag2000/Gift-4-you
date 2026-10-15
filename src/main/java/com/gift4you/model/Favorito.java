package com.gift4you.model;

import java.time.LocalDateTime;

public class Favorito implements Identificavel, PertencePessoa {

    private Integer id;
    private final int pessoaId;
    private final OrigemSugestao origem;
    private final ItemSugerido item;
    private final LocalDateTime favoritadoEm;

    public Favorito(int pessoaId, OrigemSugestao origem, ItemSugerido item, LocalDateTime favoritadoEm) {
        this.pessoaId = pessoaId;
        this.origem = origem;
        this.item = item;
        this.favoritadoEm = favoritadoEm;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public int getPessoaId() {
        return pessoaId;
    }

    public OrigemSugestao getOrigem() {
        return origem;
    }

    public ItemSugerido getItem() {
        return item;
    }

    public LocalDateTime getFavoritadoEm() {
        return favoritadoEm;
    }

    /**
     * Itens do catálogo são o mesmo presente pelo id; ideias da IA, pelo nome.
     */
    public boolean refereSeA(OrigemSugestao outraOrigem, ItemSugerido outroItem) {
        if (origem != outraOrigem) {
            return false;
        }
        return origem == OrigemSugestao.CATALOGO
                ? item.presenteId().equals(outroItem.presenteId())
                : item.nome().equalsIgnoreCase(outroItem.nome().trim());
    }
}
