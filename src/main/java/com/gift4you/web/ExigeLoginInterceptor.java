package com.gift4you.web;

import com.gift4you.exception.NaoAutenticadoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Recusa com 401 os pedidos à API feitos sem login. Garante a proteção mesmo nas rotas que não
 * usam {@link UsuarioLogado}, como o catálogo de presentes.
 */
public class ExigeLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest requisicao, HttpServletResponse resposta, Object handler) {
        if (SessaoUsuario.usuarioLogado(requisicao).isEmpty()) {
            throw new NaoAutenticadoException();
        }
        return true;
    }
}
