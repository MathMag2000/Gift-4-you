package com.gift4you.config;

import com.gift4you.integracao.ExtratorImagemPaginaWeb;
import com.gift4you.integracao.GeminiGeradorIdeias;
import com.gift4you.integracao.LeitorBuscaAmazon;
import com.gift4you.integracao.LeitorImagemHtml;
import com.gift4you.integracao.LojaAmazon;
import com.gift4you.integracao.ProtecaoEnderecoInterno;
import com.gift4you.repository.FavoritoRepositoryJdbc;
import com.gift4you.repository.HistoricoRepositoryJdbc;
import com.gift4you.repository.PessoaRepositoryJdbc;
import com.gift4you.repository.PresenteRepositoryJdbc;
import com.gift4you.repository.UsuarioRepositoryJdbc;
import com.gift4you.service.CodificadorSenhaPbkdf2;
import com.gift4you.service.ComparadorTermos;
import com.gift4you.service.ExtratorImagemProduto;
import com.gift4you.service.FavoritoService;
import com.gift4you.service.FavoritoValidador;
import com.gift4you.service.GeradorIdeias;
import com.gift4you.service.GeradorIdeiasIndisponivel;
import com.gift4you.service.HistoricoService;
import com.gift4you.service.LimiteDeTaxa;
import com.gift4you.service.LimitesConta;
import com.gift4you.service.LojaOnline;
import com.gift4you.service.PessoaService;
import com.gift4you.service.PessoaValidador;
import com.gift4you.service.PresenteService;
import com.gift4you.service.PresenteValidador;
import com.gift4you.service.SugestaoService;
import com.gift4you.service.UsuarioService;
import com.gift4you.service.UsuarioValidador;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;

/**
 * Monta as classes de negócio. Elas não dependem do Spring, que só fica responsável por ligá-las.
 * Os dados ficam no PostgreSQL configurado em application.properties (veja o README).
 */
@Configuration
public class AplicacaoConfig {

    /**
     * Limites contra abuso (ver README). Os valores padrão servem para o site publicado; os testes os
     * aumentam, pois criam muitas contas a partir do mesmo endereço.
     */
    @Bean
    public LimitesConta limitesConta(Clock relogio,
                                     @Value("${gift4you.limite.login-falhas-por-email:5}") int falhasPorEmail,
                                     @Value("${gift4you.limite.login-falhas-por-ip:20}") int falhasPorIp,
                                     @Value("${gift4you.limite.cadastros-por-ip-hora:10}") int cadastrosPorIp) {
        Duration quinzeMinutos = Duration.ofMinutes(15);
        return new LimitesConta(new LimiteDeTaxa(falhasPorEmail, quinzeMinutos, relogio),
                new LimiteDeTaxa(falhasPorIp, quinzeMinutos, relogio),
                new LimiteDeTaxa(cadastrosPorIp, Duration.ofHours(1), relogio));
    }

    @Bean
    public UsuarioService usuarioService(JdbcClient jdbc, LimitesConta limitesConta,
                                         @Value("${gift4you.senha.iteracoes:" + CodificadorSenhaPbkdf2.ITERACOES_PADRAO + "}")
                                         int iteracoesSenha) {
        return new UsuarioService(new UsuarioRepositoryJdbc(jdbc), new UsuarioValidador(),
                new CodificadorSenhaPbkdf2(iteracoesSenha), limitesConta);
    }

    @Bean
    public PessoaService pessoaService(JdbcClient jdbc) {
        return new PessoaService(new PessoaRepositoryJdbc(jdbc), new PessoaValidador());
    }

    @Bean
    public ExtratorImagemProduto extratorImagemProduto() {
        return new ExtratorImagemPaginaWeb(new ProtecaoEnderecoInterno(), new LeitorImagemHtml());
    }

    @Bean
    public PresenteService presenteService(JdbcClient jdbc, ExtratorImagemProduto extratorImagemProduto) {
        return new PresenteService(new PresenteRepositoryJdbc(jdbc), new PresenteValidador(), extratorImagemProduto);
    }

    @Bean
    public GeradorIdeias geradorIdeias(@Value("${gemini.chave-api:}") String chaveApi,
                                       @Value("${gemini.modelo:}") String modelo) {
        if (chaveApi.isBlank()) {
            return new GeradorIdeiasIndisponivel(
                    "IA não configurada. Crie o arquivo chave-ia.properties (veja o README) e reinicie o servidor.");
        }
        return new GeminiGeradorIdeias(chaveApi, modelo);
    }

    @Bean
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public HistoricoService historicoService(JdbcClient jdbc, TransactionTemplate transacao,
                                             PessoaService pessoaService, Clock relogio) {
        HistoricoService historicoService = new HistoricoService(new HistoricoRepositoryJdbc(jdbc, transacao),
                pessoaService, relogio);
        pessoaService.adicionarOuvinteRemocao(historicoService);
        return historicoService;
    }

    /** Loja onde as ideias da IA são procuradas. */
    @Bean
    public LojaOnline lojaOnline() {
        return new LojaAmazon(new LeitorBuscaAmazon());
    }

    @Bean
    public FavoritoService favoritoService(JdbcClient jdbc, PessoaService pessoaService,
                                           PresenteService presenteService, LojaOnline lojaOnline, Clock relogio) {
        FavoritoService favoritoService = new FavoritoService(new FavoritoRepositoryJdbc(jdbc),
                new FavoritoValidador(), pessoaService, presenteService, lojaOnline, relogio);
        pessoaService.adicionarOuvinteRemocao(favoritoService);
        return favoritoService;
    }

    @Bean
    public SugestaoService sugestaoService(PessoaService pessoaService, PresenteService presenteService,
                                           GeradorIdeias geradorIdeias, HistoricoService historicoService,
                                           LojaOnline lojaOnline, Clock relogio,
                                           @Value("${gift4you.limite.ia-por-hora:10}") int pedidosIaPorHora) {
        return new SugestaoService(pessoaService, presenteService, new ComparadorTermos(), geradorIdeias,
                historicoService, lojaOnline, new LimiteDeTaxa(pedidosIaPorHora, Duration.ofHours(1), relogio));
    }
}
