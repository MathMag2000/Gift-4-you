package com.gift4you.model;

/**
 * Pedido de troca de senha: a senha atual confirma que é o próprio dono da conta quem está pedindo.
 */
public record DadosAlteracaoSenha(String senhaAtual, String novaSenha) {

    /** Evita que as senhas apareçam em logs caso o objeto seja impresso. */
    @Override
    public String toString() {
        return "DadosAlteracaoSenha[***]";
    }
}
