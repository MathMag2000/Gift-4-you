package com.gift4you.web;

import com.gift4you.exception.NaoAutenticadoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Preenche os parâmetros {@link UsuarioLogado} dos controladores com o usuário da sessão. */
public class UsuarioLogadoResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return parametro.getParameterType().equals(UsuarioLogado.class);
    }

    @Override
    public UsuarioLogado resolveArgument(MethodParameter parametro, ModelAndViewContainer modelo,
                                         NativeWebRequest requisicao, WebDataBinderFactory fabricaBinder) {
        return SessaoUsuario.usuarioLogado(requisicao.getNativeRequest(HttpServletRequest.class))
                .orElseThrow(NaoAutenticadoException::new);
    }
}
