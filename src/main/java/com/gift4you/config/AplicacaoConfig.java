package com.gift4you.config;

import com.gift4you.integracao.GeminiGeradorIdeias;
import com.gift4you.repository.PessoaRepositoryEmMemoria;
import com.gift4you.repository.PresenteRepositoryEmMemoria;
import com.gift4you.service.ComparadorTermos;
import com.gift4you.service.GeradorIdeias;
import com.gift4you.service.GeradorIdeiasIndisponivel;
import com.gift4you.service.PessoaService;
import com.gift4you.service.PessoaValidador;
import com.gift4you.service.PresenteService;
import com.gift4you.service.PresenteValidador;
import com.gift4you.service.SugestaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta as classes de negócio. Elas não dependem do Spring, que só fica responsável por ligá-las.
 */
@Configuration
public class AplicacaoConfig {

    @Bean
    public PessoaService pessoaService() {
        return new PessoaService(new PessoaRepositoryEmMemoria(), new PessoaValidador());
    }

    @Bean
    public PresenteService presenteService() {
        return new PresenteService(new PresenteRepositoryEmMemoria(), new PresenteValidador());
    }

    @Bean
    public GeradorIdeias geradorIdeias(@Value("${gemini.chave-api:}") String chaveApi,
                                       @Value("${gemini.modelo:}") String modelo) {
        if (chaveApi.isBlank()) {
            return new GeradorIdeiasIndisponivel(
                    "IA não configurada. Defina a variável de ambiente GEMINI_API_KEY e reinicie o servidor.");
        }
        return new GeminiGeradorIdeias(chaveApi, modelo);
    }

    @Bean
    public SugestaoService sugestaoService(PessoaService pessoaService, PresenteService presenteService,
                                           GeradorIdeias geradorIdeias) {
        return new SugestaoService(pessoaService, presenteService, new ComparadorTermos(), geradorIdeias);
    }
}
