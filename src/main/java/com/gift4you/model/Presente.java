package com.gift4you.model;

import java.math.BigDecimal;
import java.util.List;

public class Presente implements Identificavel {

    private Integer id;
    private String nome;
    private Categoria categoria;
    private String descricao;
    private BigDecimal preco;
    private List<String> caracteristicas;
    private List<Ocasiao> ocasioes;
    private String linkCompra;
    private String imagemUrl;

    public Presente(DadosPresente dados) {
        atualizar(dados);
    }

    public void atualizar(DadosPresente dados) {
        this.nome = dados.nome();
        this.categoria = dados.categoria();
        this.descricao = dados.descricao();
        this.preco = dados.preco();
        this.caracteristicas = List.copyOf(dados.caracteristicas());
        this.ocasioes = List.copyOf(dados.ocasioes());
        this.linkCompra = dados.linkCompra();
    }

    /**
     * Imagem obtida da página do link de compra; nula quando a página não pôde ser lida.
     */
    public void definirImagem(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public List<String> getCaracteristicas() {
        return caracteristicas;
    }

    public List<Ocasiao> getOcasioes() {
        return ocasioes;
    }

    public String getLinkCompra() {
        return linkCompra;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nome + " (" + categoria + ")";
    }
}
