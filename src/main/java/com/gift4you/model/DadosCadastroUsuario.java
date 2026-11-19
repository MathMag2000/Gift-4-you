package com.gift4you.model;

public record DadosCadastroUsuario(String nome, String email, String senha) {

    /** Evita que a senha apareça em logs caso o objeto seja impresso. */
    @Override
    public String toString() {
        return "DadosCadastroUsuario[nome=" + nome + ", email=" + email + "]";
    }
}
