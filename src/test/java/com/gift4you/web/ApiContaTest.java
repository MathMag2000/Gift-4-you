package com.gift4you.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadastro, login e logout, e a separação dos dados entre contas.
 */
class ApiContaTest extends TesteComBanco {

    private static final String PESSOA = """
            {"nome":"Ana","idade":30,"vinculo":"AMIGO","gostos":[],"naoGosta":[],
             "ocasiao":"NATAL","orcamento":{"maximo":100}}""";

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder pedido, String corpo) {
        return pedido.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    @Test
    void cadastroEntraNaContaENaoDevolveASenha() throws Exception {
        MvcResult resultado = mvc.perform(json(post("/api/conta/cadastro"), """
                        {"nome":" Marcos ","email":" Marcos@Exemplo.COM ","senha":"senha1234"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Marcos"))
                .andExpect(jsonPath("$.email").value("marcos@exemplo.com"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaHash").doesNotExist())
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);

        mvc.perform(get("/api/conta").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("marcos@exemplo.com"));
    }

    @Test
    void recusaCadastroComDadosInvalidosOuEmailRepetido() throws Exception {
        cadastrarConta("Marcos", "marcos@exemplo.com");

        mvc.perform(json(post("/api/conta/cadastro"), """
                        {"nome":"Outro","email":"MARCOS@exemplo.com","senha":"senha1234"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe uma conta com este e-mail."));
        mvc.perform(json(post("/api/conta/cadastro"), """
                        {"nome":"Bia","email":"bia@","senha":"senha1234"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe um e-mail válido, como nome@exemplo.com."));
        mvc.perform(json(post("/api/conta/cadastro"), """
                        {"nome":"Bia","email":"bia@exemplo.com","senha":"curta1"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A senha deve ter pelo menos 8 caracteres."));
    }

    @Test
    void loginComSenhaCertaEntraEComSenhaErradaNao() throws Exception {
        cadastrarConta("Marcos", "marcos@exemplo.com");

        mvc.perform(json(post("/api/conta/login"), """
                        {"email":"marcos@exemplo.com","senha":"errada123"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos."));
        mvc.perform(json(post("/api/conta/login"), """
                        {"email":"ninguem@exemplo.com","senha":"senha1234"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos."));

        MvcResult resultado = mvc.perform(json(post("/api/conta/login"), """
                        {"email":"MARCOS@exemplo.com","senha":"senha1234"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Marcos"))
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) resultado.getRequest().getSession(false);

        mvc.perform(get("/api/pessoas").session(sessao)).andExpect(status().isOk());
    }

    @Test
    void loginTrocaASessaoParaEvitarFixacao() throws Exception {
        cadastrarConta("Marcos", "marcos@exemplo.com");
        MockHttpSession anterior = new MockHttpSession();

        MvcResult resultado = mvc.perform(json(post("/api/conta/login").session(anterior), """
                        {"email":"marcos@exemplo.com","senha":"senha1234"}"""))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(anterior.isInvalid()).isTrue();
        assertThat(resultado.getRequest().getSession(false)).isNotSameAs(anterior);
    }

    @Test
    void semLoginAApiRespondeNaoAutenticado() throws Exception {
        mvc.perform(get("/api/conta")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pessoas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Faça login para continuar."));
        mvc.perform(get("/api/presentes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/favoritos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/historico")).andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/pessoas"), PESSOA)).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/opcoes")).andExpect(status().isOk());
    }

    @Test
    void logoutEncerraASessao() throws Exception {
        MockHttpSession sessao = cadastrarConta("Marcos", "marcos@exemplo.com");

        mvc.perform(post("/api/conta/logout").session(sessao)).andExpect(status().isNoContent());

        assertThat(sessao.isInvalid()).isTrue();
        mvc.perform(get("/api/pessoas").session(new MockHttpSession())).andExpect(status().isUnauthorized());
    }

    @Test
    void cadaContaVeSomenteAsSuasPessoas() throws Exception {
        MockHttpSession marcos = cadastrarConta("Marcos", "marcos@exemplo.com");
        MockHttpSession bia = cadastrarConta("Bia", "bia@exemplo.com");
        mvc.perform(json(post("/api/pessoas").session(marcos), PESSOA)).andExpect(status().isCreated());
        mvc.perform(post("/api/favoritos").session(marcos).contentType(MediaType.APPLICATION_JSON).content("""
                        {"pessoaId":1,"origem":"IA","item":{"nome":"Caneca","preco":40}}"""))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/pessoas").session(marcos)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/pessoas").session(bia)).andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(get("/api/pessoas/1").session(bia)).andExpect(status().isNotFound());
        mvc.perform(json(put("/api/pessoas/1").session(bia), PESSOA)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/pessoas/1").session(bia)).andExpect(status().isNotFound());
        mvc.perform(get("/api/pessoas/1/sugestoes").session(bia)).andExpect(status().isNotFound());
        mvc.perform(get("/api/favoritos").session(bia)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/favoritos").param("pessoaId", "1").session(bia)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(delete("/api/favoritos/1").session(bia)).andExpect(status().isNotFound());
        mvc.perform(post("/api/favoritos").session(bia).contentType(MediaType.APPLICATION_JSON).content("""
                        {"pessoaId":1,"origem":"IA","item":{"nome":"Caneca","preco":40}}"""))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/pessoas/1").session(marcos)).andExpect(status().isOk());
        mvc.perform(get("/api/favoritos").session(marcos)).andExpect(jsonPath("$", hasSize(1)));
    }
}
