package com.gift4you.web;

import com.gift4you.service.ExtratorImagemProduto;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base dos testes de API: sobe um PostgreSQL de verdade (embutido, sem instalação) uma vez para todos os
 * testes, e esvazia as tabelas antes de cada um, para que os ids comecem sempre em 1.
 */
@SpringBootTest(properties = {"gemini.chave-api=", "gift4you.senha.iteracoes=1000",
        // Os testes criam muitas contas e erram senhas de propósito, todos a partir do mesmo endereço.
        "gift4you.limite.cadastros-por-ip-hora=10000", "gift4you.limite.login-falhas-por-ip=10000",
        "gift4you.limite.login-falhas-por-email=10000"})
@AutoConfigureMockMvc
abstract class TesteComBanco {

    private static final EmbeddedPostgres BANCO = iniciarBanco();

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcClient jdbc;

    /** Evita acessar a internet: a página da loja é simulada. */
    @MockitoBean
    protected ExtratorImagemProduto extratorImagem;

    private static EmbeddedPostgres iniciarBanco() {
        try {
            return EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível iniciar o PostgreSQL embutido dos testes.", e);
        }
    }

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry propriedades) {
        propriedades.add("spring.datasource.url", () -> BANCO.getJdbcUrl("postgres", "postgres"));
        propriedades.add("spring.datasource.username", () -> "postgres");
        propriedades.add("spring.datasource.password", () -> "postgres");
    }

    @BeforeEach
    void esvaziarBanco() {
        jdbc.sql("TRUNCATE usuario, pessoa, presente, favorito, registro_historico, item_historico "
                + "RESTART IDENTITY CASCADE").update();
    }

    /** Cadastra uma conta e devolve a sessão já logada nela. */
    protected MockHttpSession cadastrarConta(String nome, String email) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/conta/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","email":"%s","senha":"senha1234"}""".formatted(nome, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return (MockHttpSession) resultado.getRequest().getSession(false);
    }
}
