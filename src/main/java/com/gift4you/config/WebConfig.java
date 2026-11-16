package com.gift4you.config;

import com.gift4you.web.ExigeLoginInterceptor;
import com.gift4you.web.UsuarioLogadoResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Exige login em toda a API, exceto na conta (cadastro, login, logout e consulta da sessão) e nas
 * opções dos formulários. As páginas do site continuam públicas: é o site que mostra a tela de login.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new ExigeLoginInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/conta", "/api/conta/**", "/api/opcoes");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UsuarioLogadoResolver());
    }
}
