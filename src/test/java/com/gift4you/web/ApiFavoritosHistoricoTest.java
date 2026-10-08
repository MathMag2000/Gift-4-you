package com.gift4you.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre a API como o site faz: cadastra, gera sugestões, consulta o histórico e favorita.
 */
@SpringBootTest(properties = "gemini.chave-api=")
@AutoConfigureMockMvc
class ApiFavoritosHistoricoTest {

    @Autowired
    private MockMvc mvc;

    private void enviar(String caminho, String json) throws Exception {
        mvc.perform(post(caminho).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void fluxoDeSugestaoHistoricoEFavorito() throws Exception {
        enviar("/api/pessoas", """
                {"nome":"Ana","idade":30,"vinculo":"AMIGO","gostos":["café"],"interesses":[],"naoGosta":[],
                 "ocasiao":"ANIVERSARIO","orcamento":{"minimo":50,"maximo":200}}""");
        enviar("/api/presentes", """
                {"nome":"Kit de café","categoria":"GASTRONOMIA","descricao":"","preco":90,
                 "caracteristicas":["café"],"ocasioes":["ANIVERSARIO"]}""");

        mvc.perform(get("/api/pessoas/1/sugestoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(get("/api/historico").param("pessoaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origem").value("CATALOGO"))
                .andExpect(jsonPath("$[0].ocasiao").value("ANIVERSARIO"))
                .andExpect(jsonPath("$[0].itens[0].nome").value("Kit de café"))
                .andExpect(jsonPath("$[0].itens[0].categoria").value("Gastronomia"))
                .andExpect(jsonPath("$[0].realizadoEm").isString());

        mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"pessoaId":1,"origem":"CATALOGO","item":{"presenteId":1,"motivos":["combina"]}}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.item.nome").value("Kit de café"))
                .andExpect(jsonPath("$.origem").value("CATALOGO"));

        mvc.perform(get("/api/favoritos").param("pessoaId", "1"))
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete("/api/favoritos/1")).andExpect(status().isNoContent());
        mvc.perform(delete("/api/favoritos/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Favorito com id 1 não encontrado."));

        mvc.perform(post("/api/favoritos").contentType(MediaType.APPLICATION_JSON).content("""
                        {"pessoaId":1,"origem":"IA","item":{"nome":"","preco":10}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O nome da ideia é obrigatório."));

        mvc.perform(get("/api/historico/99")).andExpect(status().isNotFound());
        mvc.perform(get("/api/opcoes")).andExpect(jsonPath("$.origens[1].descricao").value("IA"));
    }
}
