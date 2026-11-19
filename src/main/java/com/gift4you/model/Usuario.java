package com.gift4you.model;

/**
 * Conta de acesso ao sistema. Cada usuário vê somente as pessoas que ele mesmo cadastrou.
 * A senha nunca é guardada: somente o hash dela, gerado por {@code CodificadorSenha}.
 */
public class Usuario implements Identificavel {

    private Integer id;
    private final String nome;
    private final String email;
    private final String senhaHash;

    public Usuario(String nome, String email, String senhaHash) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
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

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nome + " <" + email + ">";
    }
}
