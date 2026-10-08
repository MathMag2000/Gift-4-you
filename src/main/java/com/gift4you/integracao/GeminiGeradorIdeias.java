package com.gift4you.integracao;

import com.gift4you.exception.IntegracaoIaException;
import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Pessoa;
import com.gift4you.service.GeradorIdeias;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gera ideias de presentes com a API do Gemini. Envia apenas o perfil da pessoa, nunca o nome.
 */
public class GeminiGeradorIdeias implements GeradorIdeias {

    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODELO_PADRAO = "gemini-flash-latest";
    private static final String MODELO_RESERVA = "gemini-flash-lite-latest";
    private static final Set<Integer> STATUS_TENTAR_OUTRO_MODELO = Set.of(404, 429, 500, 503);

    private static final String ESQUEMA_RESPOSTA = """
            {"type":"ARRAY","items":{"type":"OBJECT","properties":{
            "nome":{"type":"STRING"},"categoria":{"type":"STRING"},
            "precoEstimado":{"type":"NUMBER"},"motivo":{"type":"STRING"}},
            "required":["nome","categoria","precoEstimado","motivo"]}}""";

    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String chaveApi;
    private final List<String> modelos;

    /**
     * @param modelo modelo preferido; se nulo ou vazio, usa {@value MODELO_PADRAO}
     */
    public GeminiGeradorIdeias(String chaveApi, String modelo) {
        this.chaveApi = chaveApi;
        Set<String> ordem = new LinkedHashSet<>();
        ordem.add(modelo == null || modelo.isBlank() ? MODELO_PADRAO : modelo.trim());
        ordem.add(MODELO_RESERVA);
        this.modelos = List.copyOf(ordem);
    }

    @Override
    public List<IdeiaPresente> gerarIdeias(Pessoa pessoa, int quantidade) {
        String corpo = montarCorpo(montarPrompt(pessoa, quantidade));
        HttpResponse<String> resposta = null;
        for (String modelo : modelos) {
            resposta = enviar(modelo, corpo);
            if (!STATUS_TENTAR_OUTRO_MODELO.contains(resposta.statusCode())) {
                break;
            }
        }
        if (resposta.statusCode() != 200) {
            throw new IntegracaoIaException(mensagemDeErro(resposta.statusCode()));
        }
        return converterIdeias(extrairTexto(resposta.body()));
    }

    private String montarPrompt(Pessoa pessoa, int quantidade) {
        return """
                Você sugere presentes para pessoas no Brasil.
                Sugira %d ideias de presentes diferentes para a pessoa descrita abaixo.

                Regras:
                - O preço estimado, em reais, deve estar entre %s e %s.
                - %s
                - O presente deve ser adequado para a ocasião e para o vínculo.
                - Priorize os gostos e interesses informados.
                - O motivo deve ter uma frase curta, em português.

                Perfil:
                - Idade: %d anos
                - Vínculo com quem vai presentear: %s
                - Gostos: %s
                - Interesses: %s
                - Ocasião: %s
                """.formatted(quantidade,
                pessoa.getOrcamento().minimo().toPlainString(), pessoa.getOrcamento().maximo().toPlainString(),
                pessoa.getNaoGosta().isEmpty() ? "Não há itens a evitar."
                        : "Não sugira nada relacionado a: " + String.join(", ", pessoa.getNaoGosta()) + ".",
                pessoa.getIdade(), pessoa.getVinculo(),
                listaOuNenhum(pessoa.getGostos()), listaOuNenhum(pessoa.getInteresses()), pessoa.getOcasiao());
    }

    private String montarCorpo(String prompt) {
        return "{\"contents\":[{\"parts\":[{\"text\":" + Json.escrever(prompt) + "}]}],"
                + "\"generationConfig\":{\"responseMimeType\":\"application/json\","
                + "\"responseSchema\":" + ESQUEMA_RESPOSTA + "}}";
    }

    private HttpResponse<String> enviar(String modelo, String corpo) {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(URL_BASE + modelo + ":generateContent"))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", chaveApi)
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        try {
            return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IntegracaoIaException("Não foi possível conectar ao Gemini. Verifique sua internet.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegracaoIaException("A consulta ao Gemini foi interrompida.", e);
        }
    }

    private String mensagemDeErro(int status) {
        return switch (status) {
            case 400, 401, 403 -> "O Gemini recusou a chave de API. Confira a variável GEMINI_API_KEY.";
            case 404 -> "Modelo do Gemini não encontrado. Confira a variável GEMINI_MODEL.";
            case 429 -> "Limite de uso do Gemini atingido. Tente novamente mais tarde.";
            case 500, 503 -> "O Gemini está sobrecarregado no momento. Tente novamente em instantes.";
            default -> "O Gemini respondeu com erro (código " + status + ").";
        };
    }

    @SuppressWarnings("unchecked")
    private String extrairTexto(String corpoResposta) {
        try {
            Map<String, Object> raiz = (Map<String, Object>) Json.ler(corpoResposta);
            List<Object> candidatos = (List<Object>) raiz.get("candidates");
            if (candidatos == null || candidatos.isEmpty()) {
                throw new IntegracaoIaException("O Gemini não retornou sugestões para este perfil.");
            }
            Map<String, Object> conteudo = (Map<String, Object>) ((Map<String, Object>) candidatos.getFirst()).get("content");
            StringBuilder texto = new StringBuilder();
            for (Object parte : (List<Object>) conteudo.get("parts")) {
                Map<String, Object> mapaParte = (Map<String, Object>) parte;
                if (!Boolean.TRUE.equals(mapaParte.get("thought")) && mapaParte.get("text") instanceof String trecho) {
                    texto.append(trecho);
                }
            }
            return texto.toString();
        } catch (ClassCastException | NullPointerException | IllegalArgumentException e) {
            throw new IntegracaoIaException("Resposta do Gemini em formato inesperado.", e);
        }
    }

    private List<IdeiaPresente> converterIdeias(String textoJson) {
        Object conteudo;
        try {
            conteudo = Json.ler(textoJson);
        } catch (IllegalArgumentException e) {
            throw new IntegracaoIaException("O Gemini retornou ideias em formato inesperado.", e);
        }
        if (!(conteudo instanceof List<?> itens)) {
            throw new IntegracaoIaException("O Gemini retornou ideias em formato inesperado.");
        }
        List<IdeiaPresente> ideias = new ArrayList<>();
        for (Object item : itens) {
            if (item instanceof Map<?, ?> ideia
                    && ideia.get("nome") instanceof String nome
                    && ideia.get("precoEstimado") instanceof BigDecimal preco) {
                ideias.add(new IdeiaPresente(nome, textoOuVazio(ideia.get("categoria")), preco,
                        textoOuVazio(ideia.get("motivo"))));
            }
        }
        return ideias;
    }

    private String textoOuVazio(Object valor) {
        return valor instanceof String texto ? texto : "";
    }

    private String listaOuNenhum(List<String> itens) {
        return itens.isEmpty() ? "nada informado" : String.join(", ", itens);
    }
}
