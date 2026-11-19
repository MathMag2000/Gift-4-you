package com.gift4you.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proteções do site publicado: catálogo separado por conta, cabeçalhos de segurança e limite da IA.
 */
class ApiSegurancaTest extends TesteComBanco {

    private static final String PRESENTE = """
            {"nome":"Kit de café","categoria":"GASTRONOMIA","preco":90,"caracteristicas":["café"],
             "ocasioes":["NATAL"],"linkCompra":"https://loja.exemplo.com/kit"}""";

    @Test
    void cadaContaTemOProprioCatalogo() throws Exception {
        given(extratorImagem.extrair(anyString())).willReturn(Optional.empty());
        MockHttpSession marcos = cadastrarConta("Marcos", "marcos@exemplo.com");
        MockHttpSession bia = cadastrarConta("Bia", "bia@exemplo.com");
        mvc.perform(post("/api/presentes").session(marcos).contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/presentes").session(marcos)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/presentes").session(bia)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/presentes/1").session(bia)).andExpect(status().isNotFound());
        mvc.perform(put("/api/presentes/1").session(bia).contentType(MediaType.APPLICATION_JSON).content(PRESENTE))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/presentes/1").session(bia)).andExpect(status().isNotFound());

        mvc.perform(get("/api/presentes/1").session(marcos)).andExpect(status().isOk());
    }

    @Test
    void enviaCabecalhosDeSegurancaNoSiteENaApi() throws Exception {
        for (String caminho : new String[]{"/", "/api/opcoes"}) {
            mvc.perform(get(caminho))
                    .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                    .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
        }
        mvc.perform(get("/")).andExpect(header().doesNotExist("Strict-Transport-Security"));
        mvc.perform(get("/").secure(true)).andExpect(header().exists("Strict-Transport-Security"));
    }

    /** Sem chave da IA nos testes, cada pedido falha com 503, mas conta no limite (10 por hora). */
    @Test
    void pedidosDeIaAlemDoLimiteRecebem429ComRetryAfter() throws Exception {
        MockHttpSession sessao = cadastrarConta("Marcos", "marcos@exemplo.com");
        mvc.perform(post("/api/pessoas").session(sessao).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Ana","idade":30,"vinculo":"AMIGO","gostos":[],"naoGosta":[],
                         "ocasiao":"NATAL","orcamento":{"maximo":100}}"""))
                .andExpect(status().isCreated());

        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/pessoas/1/ideias-ia").session(sessao)).andExpect(status().isServiceUnavailable());
        }

        mvc.perform(post("/api/pessoas/1/ideias-ia").session(sessao))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.mensagem").value(
                        "Você atingiu o limite de 10 pedidos de ideias com IA por hora. Tente novamente em 60 minutos."));
    }
}
