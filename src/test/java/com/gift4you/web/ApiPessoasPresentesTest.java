package com.gift4you.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadastro, alteração e remoção de pessoas e presentes pela API, gravando no PostgreSQL.
 */
class ApiPessoasPresentesTest extends TesteComBanco {

    private static final String ANA = """
            {"nome":"  Ana   Maria ","idade":30,"vinculo":"AMIGO","gostos":["Café","café","música"],
             "naoGosta":["perfume"],"ocasiao":"ANIVERSARIO","orcamento":{"maximo":199.90}}""";

    private static final String FONE = """
            {"nome":"Fone","categoria":"ELETRONICOS","descricao":"Sem fio","preco":150,
             "caracteristicas":["música"],"ocasioes":["NATAL","ANIVERSARIO"],"linkCompra":"https://loja.exemplo.com/fone"}""";

    private MockHttpSession sessao;

    @BeforeEach
    void entrar() throws Exception {
        sessao = cadastrarConta("Marcos", "marcos@exemplo.com");
        given(extratorImagem.extrair(anyString())).willReturn(Optional.empty());
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder pedido, String corpo) {
        return pedido.session(sessao).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    @Test
    void cadastraAlteraERemovePessoa() throws Exception {
        mvc.perform(json(post("/api/pessoas"), ANA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Ana Maria"))
                .andExpect(jsonPath("$.gostos", hasSize(2)));

        mvc.perform(get("/api/pessoas/1").session(sessao))
                .andExpect(jsonPath("$.nome").value("Ana Maria"))
                .andExpect(jsonPath("$.idade").value(30))
                .andExpect(jsonPath("$.vinculo").value("AMIGO"))
                .andExpect(jsonPath("$.gostos[0]").value("café"))
                .andExpect(jsonPath("$.gostos[1]").value("música"))
                .andExpect(jsonPath("$.naoGosta[0]").value("perfume"))
                .andExpect(jsonPath("$.ocasiao").value("ANIVERSARIO"))
                .andExpect(jsonPath("$.orcamento.maximo").value(199.90));

        mvc.perform(json(put("/api/pessoas/1"), """
                        {"nome":"Ana","idade":31,"vinculo":"IRMAO","gostos":[],"naoGosta":[],
                         "ocasiao":"NATAL","orcamento":{"maximo":80}}"""))
                .andExpect(status().isOk());
        mvc.perform(get("/api/pessoas/1").session(sessao))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.idade").value(31))
                .andExpect(jsonPath("$.vinculo").value("IRMAO"))
                .andExpect(jsonPath("$.gostos", hasSize(0)))
                .andExpect(jsonPath("$.ocasiao").value("NATAL"))
                .andExpect(jsonPath("$.orcamento.maximo").value(80));

        mvc.perform(delete("/api/pessoas/1").session(sessao)).andExpect(status().isNoContent());
        mvc.perform(get("/api/pessoas/1").session(sessao)).andExpect(status().isNotFound());
        mvc.perform(get("/api/pessoas").session(sessao)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(delete("/api/pessoas/1").session(sessao)).andExpect(status().isNotFound());
    }

    @Test
    void guardaEstiloEInformacoesAdicionaisNoBanco() throws Exception {
        mvc.perform(json(post("/api/pessoas"), ANA.replace("\"ocasiao\"",
                        "\"estilos\":[\"prático\",\"criativo\"],\"observacoes\":\"Mudou de casa.\",\"ocasiao\"")))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/pessoas/1").session(sessao))
                .andExpect(jsonPath("$.estilos[0]").value("prático"))
                .andExpect(jsonPath("$.estilos[1]").value("criativo"))
                .andExpect(jsonPath("$.observacoes").value("Mudou de casa."));

        mvc.perform(json(put("/api/pessoas/1"), ANA)).andExpect(status().isOk());
        mvc.perform(get("/api/pessoas/1").session(sessao))
                .andExpect(jsonPath("$.estilos", hasSize(0)))
                .andExpect(jsonPath("$.observacoes").value(""));

        mvc.perform(get("/api/opcoes")).andExpect(jsonPath("$.estilos[0].valor").value("prático"));
    }

    @Test
    void recusaPessoaInvalidaSemGravar() throws Exception {
        mvc.perform(json(post("/api/pessoas"), ANA.replace("  Ana   Maria ", "Ana123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("O nome deve conter apenas letras, espaços, apóstrofo, hífen ou ponto."));
        mvc.perform(json(post("/api/pessoas"), ANA.replace("\"idade\":30", "\"idade\":130")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A idade deve estar entre 0 e 120 anos."));
        mvc.perform(json(post("/api/pessoas"), ANA.replace("199.90", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O orçamento máximo deve ser maior que zero."));
        mvc.perform(json(post("/api/pessoas"), ANA.replace("\"AMIGO\"", "\"INIMIGO\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Dados enviados em formato inválido."));

        mvc.perform(get("/api/pessoas").session(sessao)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void cadastraAlteraERemovePresente() throws Exception {
        mvc.perform(json(post("/api/presentes"), FONE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        mvc.perform(get("/api/presentes/1").session(sessao))
                .andExpect(jsonPath("$.nome").value("Fone"))
                .andExpect(jsonPath("$.categoria").value("ELETRONICOS"))
                .andExpect(jsonPath("$.descricao").value("Sem fio"))
                .andExpect(jsonPath("$.preco").value(150))
                .andExpect(jsonPath("$.caracteristicas[0]").value("música"))
                .andExpect(jsonPath("$.ocasioes[0]").value("ANIVERSARIO"))
                .andExpect(jsonPath("$.ocasioes[1]").value("NATAL"));

        given(extratorImagem.extrair("https://loja.exemplo.com/fone-pro"))
                .willReturn(Optional.of("https://img.exemplo.com/fone-pro.jpg"));
        mvc.perform(json(put("/api/presentes/1"), FONE.replace("\"Fone\"", "\"Fone Pro\"").replace("150", "250")
                        .replace("fone\"", "fone-pro\"")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/presentes/1").session(sessao))
                .andExpect(jsonPath("$.nome").value("Fone Pro"))
                .andExpect(jsonPath("$.preco").value(250))
                .andExpect(jsonPath("$.linkCompra").value("https://loja.exemplo.com/fone-pro"))
                .andExpect(jsonPath("$.imagemUrl").value("https://img.exemplo.com/fone-pro.jpg"));

        mvc.perform(delete("/api/presentes/1").session(sessao)).andExpect(status().isNoContent());
        mvc.perform(get("/api/presentes/1").session(sessao)).andExpect(status().isNotFound());
        mvc.perform(get("/api/presentes").session(sessao)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void recusaPresenteInvalido() throws Exception {
        mvc.perform(json(post("/api/presentes"), FONE.replace("\"preco\":150", "\"preco\":0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O preço deve ser maior que zero."));
        mvc.perform(json(post("/api/presentes"), FONE.replace("\"NATAL\",\"ANIVERSARIO\"", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe ao menos uma ocasião compatível."));

        mvc.perform(get("/api/presentes").session(sessao)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sugereRespeitandoOrcamentoGostosERejeicoes() throws Exception {
        mvc.perform(json(post("/api/pessoas"), ANA)).andExpect(status().isCreated());
        String presente = """
                {"nome":"%s","categoria":"OUTRA","preco":%s,"caracteristicas":["%s"],"ocasioes":["NATAL"],
                 "linkCompra":"https://loja.exemplo.com/p"}""";
        mvc.perform(json(post("/api/presentes"), presente.formatted("Caneca", "40", "café")))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/api/presentes"), presente.formatted("Cafeteira", "450", "café")))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/api/presentes"), presente.formatted("Perfume com café", "90", "café")))
                .andExpect(status().isCreated());
        mvc.perform(json(post("/api/presentes"), presente.formatted("Disco", "199.90", "música")))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/pessoas/1/sugestoes").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].presente.nome").value("Caneca"))
                .andExpect(jsonPath("$[1].presente.nome").value("Disco"));
    }
}
