package com.gift4you.web;

/**
 * Usuário da sessão atual. Declarado como parâmetro de um método de controlador, é preenchido por
 * {@link UsuarioLogadoResolver}; sem login, o pedido recebe 401.
 */
public record UsuarioLogado(int id) {
}
