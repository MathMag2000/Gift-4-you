package com.gift4you.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Percorre a API como o site faz: entra na conta, cadastra, gera sugestões, consulta o histórico e favorita.
 * Os dados passam pelo PostgreSQL.
 */
class ApiFavoritosHistoricoTest extends TesteComBanco {

    private MockHttpSession sessao;

    @BeforeEach
    void entrar() throws Exception {
        sessao = cadastrarConta("Marcos", "marcos@exemplo.com");
    }

    private void enviar(String caminho, String json) throws Exception {
        mvc.perform(post(caminho).session(sessao).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void fluxoDeSugestaoHistoricoEFavorito() throws Exception {
        given(extratorImagem.extrair("https://loja.exemplo.com/kit-cafe"))
                .willReturn(Optional.of("https://img.exemplo.com/kit-cafe.jpg"));

        enviar("/api/pessoas", """
                {"nome":"Ana","idade":30,"vinculo":"AMIGO","gostos":["café"],"naoGosta":[],
                 "ocasiao":"ANIVERSARIO","orcamento":{"maximo":200}}""");
        enviar("/api/presentes", """
                {"nome":"Kit de café","categoria":"GASTRONOMIA","descricao":"","preco":90,
                 "caracteristicas":["café"],"ocasioes":["ANIVERSARIO"],
                 "linkCompra":"https://loja.exemplo.com/kit-cafe"}""");

        mvc.perform(post("/api/presentes").session(sessao).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"X","categoria":"JOGOS","preco":10,"ocasioes":["NATAL"],"linkCompra":"javascript:alert(1)"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(
                        "Informe um link para compra válido, começando com http:// ou https://."));

        mvc.perform(get("/api/presentes/1").session(sessao))
                .andExpect(jsonPath("$.linkCompra").value("https://loja.exemplo.com/kit-cafe"))
                .andExpect(jsonPath("$.imagemUrl").value("https://img.exemplo.com/kit-cafe.jpg"))
                .andExpect(jsonPath("$.ocasioes[0]").value("ANIVERSARIO"))
                .andExpect(jsonPath("$.caracteristicas[0]").value("café"));

        mvc.perform(get("/api/pessoas/1/sugestoes").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].presente.imagemUrl").value("https://img.exemplo.com/kit-cafe.jpg"));

        mvc.perform(get("/api/historico").session(sessao).param("pessoaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(post("/api/favoritos").session(sessao).contentType(MediaType.APPLICATION_JSON).content("""
                        {"pessoaId":1,"origem":"CATALOGO","item":{"presenteId":1,"motivos":["combina"]}}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.item.nome").value("Kit de café"))
                .andExpect(jsonPath("$.item.presenteId").value(1))
                .andExpect(jsonPath("$.origem").value("CATALOGO"));

        mvc.perform(get("/api/favoritos").session(sessao).param("pessoaId", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].item.motivos[0]").value("combina"));

        mvc.perform(delete("/api/favoritos/1").session(sessao)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/favoritos/1").session(sessao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Favorito com id 1 não encontrado."));

        mvc.perform(post("/api/favoritos").session(sessao).contentType(MediaType.APPLICATION_JSON).content("""
                        {"pessoaId":1,"origem":"IA","item":{"nome":"","preco":10}}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O nome da ideia é obrigatório."));

        mvc.perform(get("/api/historico/99").session(sessao)).andExpect(status().isNotFound());
        mvc.perform(get("/api/opcoes")).andExpect(jsonPath("$.origens[1].descricao").value("IA"));
    }

    /**
     * Grava no banco um registro de ideias da IA para a pessoa 1, como o SugestaoService faz depois de uma
     * geração concluída (os testes não acessam a IA de verdade).
     */
    private void registrarIdeiasDaIa(String pessoaNome) {
        int registro = jdbc.sql("""
                        INSERT INTO registro_historico (usuario_id, pessoa_id, pessoa_nome, origem, ocasiao,
                                                        orcamento_maximo, realizado_em)
                        VALUES (1, 1, ?, 'IA', 'NATAL', 100, '2026-10-08 12:00:00') RETURNING id""")
                .param(pessoaNome).query(Integer.class).single();
        jdbc.sql("""
                        INSERT INTO item_historico (registro_id, posicao, nome, categoria, preco, link_compra)
                        VALUES (?, 0, 'Caneca de cerâmica', 'Casa', 40, 'https://www.amazon.com.br/s?k=caneca')""")
                .param(registro).update();
    }

    @Test
    void removerPessoaApagaOsFavoritosMasMantemOHistoricoIndicandoAExclusao() throws Exception {
        enviar("/api/pessoas", """
                {"nome":"Ana","idade":30,"vinculo":"AMIGO","gostos":[],"naoGosta":[],
                 "ocasiao":"NATAL","orcamento":{"maximo":100}}""");
        registrarIdeiasDaIa("Ana");
        mvc.perform(post("/api/favoritos").session(sessao).contentType(MediaType.APPLICATION_JSON).content("""
                        {"pessoaId":1,"origem":"IA","item":{"nome":"Caneca","preco":40}}"""))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/historico").session(sessao).param("pessoaId", "1"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pessoaId").value(1))
                .andExpect(jsonPath("$[0].pessoaRemovida").value(false));

        mvc.perform(delete("/api/pessoas/1").session(sessao)).andExpect(status().isNoContent());

        mvc.perform(get("/api/historico").session(sessao))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].pessoaId").doesNotExist())
                .andExpect(jsonPath("$[0].pessoaNome").value("Ana"))
                .andExpect(jsonPath("$[0].pessoaRemovida").value(true))
                .andExpect(jsonPath("$[0].origem").value("IA"))
                .andExpect(jsonPath("$[0].itens[0].nome").value("Caneca de cerâmica"));
        mvc.perform(get("/api/historico/1").session(sessao)).andExpect(jsonPath("$.pessoaNome").value("Ana"));
        mvc.perform(get("/api/favoritos").session(sessao)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void historicoDeOutraContaNaoAparece() throws Exception {
        enviar("/api/pessoas", """
                {"nome":"Ana","idade":30,"vinculo":"AMIGO","gostos":[],"naoGosta":[],
                 "ocasiao":"NATAL","orcamento":{"maximo":100}}""");
        registrarIdeiasDaIa("Ana");
        MockHttpSession outra = cadastrarConta("Bia", "bia@exemplo.com");

        mvc.perform(get("/api/historico").session(outra)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/historico/1").session(outra)).andExpect(status().isNotFound());
    }

    @Test
    void presenteCadastradoParaAPessoaApareceSoNaListaDela() throws Exception {
        enviar("/api/pessoas", """
                {"nome":"Ana","idade":30,"vinculo":"AMIGO","gostos":["café"],"naoGosta":[],
                 "ocasiao":"NATAL","orcamento":{"maximo":100}}""");
        enviar("/api/presentes", """
                {"nome":"Relógio","categoria":"MODA","preco":900,"ocasioes":["NATAL"],
                 "linkCompra":"https://loja.exemplo.com/relogio","pessoaId":1}""");

        mvc.perform(get("/api/presentes/1").session(sessao)).andExpect(jsonPath("$.pessoaId").value(1));
        mvc.perform(get("/api/pessoas/1/presentes").session(sessao))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].presente.nome").value("Relógio"));
        mvc.perform(get("/api/pessoas/1/sugestoes").session(sessao)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/historico").session(sessao)).andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(delete("/api/pessoas/1").session(sessao)).andExpect(status().isNoContent());
        mvc.perform(get("/api/presentes/1").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pessoaId").doesNotExist());
    }
}
