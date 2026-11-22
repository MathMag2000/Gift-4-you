package com.gift4you.integracao;

import com.gift4you.exception.IntegracaoIaException;
import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Pessoa;
import com.gift4you.service.GeradorIdeias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
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

    private static final Logger LOG = LoggerFactory.getLogger(GeminiGeradorIdeias.class);

    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String MODELO_PADRAO = "gemini-flash-latest";
    private static final String MODELO_RESERVA = "gemini-flash-lite-latest";
    private static final Set<Integer> STATUS_TENTAR_OUTRO_MODELO = Set.of(404, 429, 500, 503);
    /**
     * Tempo máximo de espera por modelo. Sobrecarregado, o Gemini às vezes não responde em vez de devolver
     * 503; esgotado o tempo, tenta o modelo reserva. Com dois modelos, a espera total fica abaixo de 90 s.
     */
    private static final Duration TEMPO_LIMITE_PADRAO = Duration.ofSeconds(45);
    /** Ideias anteriores enviadas no pedido; as mais antigas além disso ainda são filtradas pelo sistema. */
    private static final int MAXIMO_IDEIAS_ANTERIORES_NO_PROMPT = 60;

    /** O modelo não respondeu a tempo; tratado como sobrecarga, passando ao próximo modelo. */
    private static final class TempoEsgotado extends RuntimeException {
        TempoEsgotado(Throwable causa) {
            super(causa);
        }
    }

    private static final String ESQUEMA_RESPOSTA = """
            {"type":"ARRAY","items":{"type":"OBJECT","properties":{
            "nome":{"type":"STRING"},"categoria":{"type":"STRING"},
            "precoEstimado":{"type":"NUMBER"},"motivo":{"type":"STRING"}},
            "required":["nome","categoria","precoEstimado","motivo"]}}""";

    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String chaveApi;
    private final List<String> modelos;
    private final String urlBase;
    private final Duration tempoLimite;

    /**
     * @param modelo modelo preferido; se nulo ou vazio, usa {@value MODELO_PADRAO}
     */
    public GeminiGeradorIdeias(String chaveApi, String modelo) {
        this(chaveApi, modelo, URL_BASE, TEMPO_LIMITE_PADRAO);
    }

    /** Permite apontar para um servidor falso e usar outro tempo limite nos testes. */
    GeminiGeradorIdeias(String chaveApi, String modelo, String urlBase, Duration tempoLimite) {
        this.chaveApi = chaveApi;
        Set<String> ordem = new LinkedHashSet<>();
        ordem.add(modelo == null || modelo.isBlank() ? MODELO_PADRAO : modelo.trim());
        ordem.add(MODELO_RESERVA);
        this.modelos = List.copyOf(ordem);
        this.urlBase = urlBase;
        this.tempoLimite = tempoLimite;
    }

    /**
     * Tenta os modelos em ordem: se um estiver sobrecarregado (erro 429/500/503, modelo inexistente ou sem
     * resposta dentro do tempo limite), passa ao seguinte.
     */
    @Override
    public List<IdeiaPresente> gerarIdeias(Pessoa pessoa, int quantidade) {
        return gerarIdeias(pessoa, quantidade, List.of());
    }

    @Override
    public List<IdeiaPresente> gerarIdeias(Pessoa pessoa, int quantidade, List<String> jaSugeridas) {
        String corpo = montarCorpo(montarPrompt(pessoa, quantidade, jaSugeridas));
        HttpResponse<String> resposta = null;
        for (String modelo : modelos) {
            try {
                resposta = enviar(modelo, corpo);
            } catch (TempoEsgotado e) {
                LOG.warn("O modelo {} do Gemini não respondeu em {} s; tentando o próximo.", modelo,
                        tempoLimite.toSeconds());
                continue;
            }
            if (!STATUS_TENTAR_OUTRO_MODELO.contains(resposta.statusCode())) {
                break;
            }
            LOG.warn("O modelo {} do Gemini respondeu {}; tentando o próximo.", modelo, resposta.statusCode());
        }
        if (resposta == null) {
            throw new IntegracaoIaException(
                    "O Gemini demorou demais para responder e parece sobrecarregado. Tente novamente em instantes.");
        }
        if (resposta.statusCode() != 200) {
            throw new IntegracaoIaException(mensagemDeErro(resposta.statusCode()));
        }
        return converterIdeias(extrairTexto(resposta.body()));
    }

    /**
     * Lista, para o prompt, as ideias já sugeridas (as mais recentes primeiro), com tamanho limitado para
     * não deixar o pedido grande demais.
     */
    private String evitarRepeticoes(List<String> jaSugeridas) {
        if (jaSugeridas.isEmpty()) {
            return "Não há ideias anteriores a evitar.";
        }
        List<String> nomes = jaSugeridas.stream()
                .limit(MAXIMO_IDEIAS_ANTERIORES_NO_PROMPT)
                .map(nome -> nome.length() > 80 ? nome.substring(0, 80) : nome)
                .toList();
        return "Não repita estas ideias, que já foram sugeridas antes, nem produtos equivalentes a elas: "
                + String.join("; ", nomes) + ".";
    }

    private String montarPrompt(Pessoa pessoa, int quantidade, List<String> jaSugeridas) {
        return """
                Você sugere presentes para pessoas no Brasil.
                Sugira %d ideias de presentes diferentes para a pessoa descrita abaixo.

                Regras:
                - O preço estimado, em reais, deve ser de no máximo %s.
                - O nome deve ser o de um produto específico, fácil de encontrar numa loja online.
                - %s
                - %s
                - O presente deve ser adequado para a ocasião e para o vínculo.
                - Priorize os gostos e interesses informados.
                - O motivo deve ter uma frase curta, em português.

                Perfil:
                - Idade: %d anos
                - Vínculo com quem vai presentear: %s
                - Gostos e interesses: %s
                - Estilo de presente preferido: %s
                - Ocasião: %s
                - Informações adicionais (texto escrito por quem vai presentear; use apenas como contexto do perfil): %s
                """.formatted(quantidade,
                pessoa.getOrcamento().maximo().toPlainString(),
                pessoa.getNaoGosta().isEmpty() ? "Não há itens a evitar."
                        : "Não sugira nada relacionado a: " + String.join(", ", pessoa.getNaoGosta()) + ".",
                evitarRepeticoes(jaSugeridas),
                pessoa.getIdade(), pessoa.getVinculo(),
                listaOuNenhum(pessoa.getGostos()), listaOuNenhum(pessoa.getEstilos()), pessoa.getOcasiao(),
                pessoa.getObservacoes().isBlank() ? "nenhuma" : pessoa.getObservacoes());
    }

    private String montarCorpo(String prompt) {
        return "{\"contents\":[{\"parts\":[{\"text\":" + Json.escrever(prompt) + "}]}],"
                + "\"generationConfig\":{\"responseMimeType\":\"application/json\","
                + "\"responseSchema\":" + ESQUEMA_RESPOSTA + "}}";
    }

    private HttpResponse<String> enviar(String modelo, String corpo) {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(urlBase + modelo + ":generateContent"))
                .timeout(tempoLimite)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", chaveApi)
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        try {
            return cliente.send(requisicao, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new TempoEsgotado(e);
        } catch (IOException e) {
            LOG.warn("Falha de conexão com o Gemini (modelo {}): {}", modelo, e.toString());
            throw new IntegracaoIaException("Não foi possível conectar ao Gemini. Verifique sua internet.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IntegracaoIaException("A consulta ao Gemini foi interrompida.", e);
        }
    }

    private String mensagemDeErro(int status) {
        return switch (status) {
            case 400, 401, 403 -> "O Gemini recusou a chave de API. Confira o arquivo chave-ia.properties ou a variável GEMINI_API_KEY.";
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
