package com.gift4you.web;

import com.gift4you.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

/**
 * Guarda na sessão HTTP qual usuário está logado. A sessão fica no servidor; o navegador recebe
 * apenas o cookie com o identificador dela.
 */
public final class SessaoUsuario {

    private static final String ATRIBUTO_USUARIO = "gift4you.usuarioId";

    private SessaoUsuario() {
    }

    /**
     * Descarta a sessão anterior e cria outra, para que um identificador de sessão obtido antes do login
     * não passe a dar acesso à conta (ataque de fixação de sessão).
     */
    public static void entrar(HttpServletRequest requisicao, Usuario usuario) {
        sair(requisicao);
        requisicao.getSession(true).setAttribute(ATRIBUTO_USUARIO, usuario.getId());
    }

    public static void sair(HttpServletRequest requisicao) {
        HttpSession sessao = requisicao.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
    }

    public static Optional<UsuarioLogado> usuarioLogado(HttpServletRequest requisicao) {
        HttpSession sessao = requisicao.getSession(false);
        if (sessao == null) {
            return Optional.empty();
        }
        return sessao.getAttribute(ATRIBUTO_USUARIO) instanceof Integer id
                ? Optional.of(new UsuarioLogado(id))
                : Optional.empty();
    }
}
