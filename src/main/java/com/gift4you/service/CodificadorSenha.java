package com.gift4you.service;

/**
 * Transforma senhas em hashes que podem ser guardados no banco e confere senhas digitadas com eles.
 */
public interface CodificadorSenha {

    String codificar(String senha);

    boolean confere(String senha, String hash);
}
