package com.gift4you.model;

import java.util.List;

public class Pessoa implements Identificavel {

    private Integer id;
    /** Conta que cadastrou a pessoa; somente ela pode consultá-la ou alterá-la. */
    private final int usuarioId;
    private String nome;
    private int idade;
    private Vinculo vinculo;
    /** Gostos e interesses, num só campo. */
    private List<String> gostos;
    private List<String> naoGosta;
    private List<String> estilos;
    private Ocasiao ocasiao;
    private FaixaOrcamento orcamento;
    private String observacoes;

    public Pessoa(int usuarioId, DadosPessoa dados) {
        this.usuarioId = usuarioId;
        atualizar(dados);
    }

    public void atualizar(DadosPessoa dados) {
        this.nome = dados.nome();
        this.idade = dados.idade();
        this.vinculo = dados.vinculo();
        this.gostos = List.copyOf(dados.gostos());
        this.naoGosta = List.copyOf(dados.naoGosta());
        this.estilos = List.copyOf(dados.estilos());
        this.ocasiao = dados.ocasiao();
        this.orcamento = dados.orcamento();
        this.observacoes = dados.observacoes();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public boolean pertenceA(int idUsuario) {
        return usuarioId == idUsuario;
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

    public List<String> getNaoGosta() {
        return naoGosta;
    }

    public List<String> getEstilos() {
        return estilos;
    }

    public String getObservacoes() {
        return observacoes;
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
