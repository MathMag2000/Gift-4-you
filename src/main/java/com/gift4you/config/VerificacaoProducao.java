package com.gift4you.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * No perfil "producao", confere as variáveis do banco antes de o servidor iniciar e, se algo estiver
 * errado, para com uma mensagem clara, em vez de um erro técnico do driver do banco.
 */
public class VerificacaoProducao implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final List<String> OBRIGATORIAS = List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent evento) {
        ConfigurableEnvironment ambiente = evento.getEnvironment();
        if (!ambiente.matchesProfiles("producao")) {
            return;
        }
        List<String> faltando = new ArrayList<>();
        for (String variavel : OBRIGATORIAS) {
            String valor = ambiente.getProperty(variavel);
            if (valor == null || valor.isBlank()) {
                faltando.add(variavel);
            }
        }
        if (!faltando.isEmpty()) {
            throw new IllegalStateException("Configuração de produção incompleta. Defina as variáveis de ambiente: "
                    + String.join(", ", faltando) + ". Veja a seção \"Publicando o site\" do README.");
        }
        String url = ambiente.getProperty("DB_URL");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("DB_URL deve estar no formato jdbc:postgresql://servidor:porta/banco "
                    + "(endereços que começam com postgres:// ou postgresql:// precisam ser convertidos; "
                    + "usuário e senha vão em DB_USERNAME e DB_PASSWORD).");
        }
    }
}
