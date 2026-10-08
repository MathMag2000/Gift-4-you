package com.gift4you.model;

import java.util.List;

public class Pessoa implements Identificavel {

    private Integer id;
    private String nome;
    private int idade;
    private Vinculo vinculo;
    private List<String> gostos;
    private List<String> interesses;
    private List<String> naoGosta;
    private Ocasiao ocasiao;
    private FaixaOrcamento orcamento;

    public Pessoa(DadosPessoa dados) {
        atualizar(dados);
    }

    public void atualizar(DadosPessoa dados) {
        this.nome = dados.nome();
        this.idade = dados.idade();
        this.vinculo = dados.vinculo();
        this.gostos = List.copyOf(dados.gostos());
        this.interesses = List.copyOf(dados.interesses());
        this.naoGosta = List.copyOf(dados.naoGosta());
        this.ocasiao = dados.ocasiao();
        this.orcamento = dados.orcamento();
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

    public int getIdade() {
        return idade;
    }

    public Vinculo getVinculo() {
        return vinculo;
    }

    public List<String> getGostos() {
        return gostos;
    }

    public List<String> getInteresses() {
        return interesses;
    }

    public List<String> getNaoGosta() {
        return naoGosta;
    }

    public Ocasiao getOcasiao() {
        return ocasiao;
    }

    public FaixaOrcamento getOrcamento() {
        return orcamento;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nome + " (" + vinculo + ", " + idade + " anos)";
    }
}
