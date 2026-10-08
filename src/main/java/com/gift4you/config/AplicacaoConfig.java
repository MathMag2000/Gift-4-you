package com.gift4you.config;

import com.gift4you.integracao.ExtratorImagemPaginaWeb;
import com.gift4you.integracao.GeminiGeradorIdeias;
import com.gift4you.integracao.LeitorImagemHtml;
import com.gift4you.integracao.ProtecaoEnderecoInterno;
import com.gift4you.repository.FavoritoRepositoryEmMemoria;
import com.gift4you.repository.HistoricoRepositoryEmMemoria;
import com.gift4you.repository.PessoaRepositoryEmMemoria;
import com.gift4you.repository.PresenteRepositoryEmMemoria;
import com.gift4you.service.ComparadorTermos;
import com.gift4you.service.ExtratorImagemProduto;
import com.gift4you.service.FavoritoService;
import com.gift4you.service.FavoritoValidador;
import com.gift4you.service.GeradorIdeias;
import com.gift4you.service.GeradorIdeiasIndisponivel;
import com.gift4you.service.HistoricoService;
import com.gift4you.service.PessoaService;
import com.gift4you.service.PessoaValidador;
import com.gift4you.service.PresenteService;
import com.gift4you.service.PresenteValidador;
import com.gift4you.service.SugestaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

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
    public ExtratorImagemProduto extratorImagemProduto() {
        return new ExtratorImagemPaginaWeb(new ProtecaoEnderecoInterno(), new LeitorImagemHtml());
    }

    @Bean
    public PresenteService presenteService(ExtratorImagemProduto extratorImagemProduto) {
        return new PresenteService(new PresenteRepositoryEmMemoria(), new PresenteValidador(), extratorImagemProduto);
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
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public HistoricoService historicoService(PessoaService pessoaService, Clock relogio) {
        HistoricoService historicoService = new HistoricoService(new HistoricoRepositoryEmMemoria(), relogio);
        pessoaService.adicionarOuvinteRemocao(historicoService);
        return historicoService;
    }

    @Bean
    public FavoritoService favoritoService(PessoaService pessoaService, PresenteService presenteService,
                                           Clock relogio) {
        FavoritoService favoritoService = new FavoritoService(new FavoritoRepositoryEmMemoria(),
                new FavoritoValidador(), pessoaService, presenteService, relogio);
        pessoaService.adicionarOuvinteRemocao(favoritoService);
        return favoritoService;
    }

    @Bean
    public SugestaoService sugestaoService(PessoaService pessoaService, PresenteService presenteService,
                                           GeradorIdeias geradorIdeias, HistoricoService historicoService) {
        return new SugestaoService(pessoaService, presenteService, new ComparadorTermos(), geradorIdeias,
                historicoService);
    }
}
