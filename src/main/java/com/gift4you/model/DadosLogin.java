package com.gift4you.model;

public record DadosLogin(String email, String senha) {

    /** Evita que a senha apareça em logs caso o objeto seja impresso. */
    @Override
    public String toString() {
        return "DadosLogin[email=" + email + "]";
    }
}
