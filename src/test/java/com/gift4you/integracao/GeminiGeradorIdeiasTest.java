package com.gift4you.integracao;

import com.gift4you.exception.IntegracaoIaException;
import com.gift4you.model.DadosPessoa;
import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.Vinculo;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comportamento diante de um Gemini sobrecarregado, com um servidor falso no próprio computador.
 */
class GeminiGeradorIdeiasTest {

    private static final String PRINCIPAL = "modelo-principal";
    private static final String RESERVA = "gemini-flash-lite-latest";
    private static final String RESPOSTA_OK = """
            {"candidates":[{"content":{"parts":[{"text":"[{\\"nome\\":\\"Caneca\\",\\"categoria\\":\\"Casa\\",\
            \\"precoEstimado\\":40,\\"motivo\\":\\"Para o café\\"}]"}]}}]}""";

    /** O que o servidor falso faz para cada modelo: responder com um código ou não responder ("travar"). */
    private final Map<String, Object> comportamento = new ConcurrentHashMap<>();
    private final List<String> modelosChamados = new CopyOnWriteArrayList<>();
    private HttpServer servidor;

    @BeforeEach
    void iniciarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.createContext("/", this::responder);
        servidor.start();
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    private void responder(HttpExchange troca) throws IOException {
        String modelo = troca.getRequestURI().getPath().replaceFirst("^/", "").replaceFirst(":generateContent$", "");
        modelosChamados.add(modelo);
        Object acao = comportamento.getOrDefault(modelo, 200);
        if ("travar".equals(acao)) {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            troca.close();
            return;
        }
        int status = (int) acao;
        byte[] corpo = (status == 200 ? RESPOSTA_OK : "{\"error\":{\"code\":" + status + "}}")
                .getBytes(StandardCharsets.UTF_8);
        troca.sendResponseHeaders(status, corpo.length);
        troca.getResponseBody().write(corpo);
        troca.close();
    }

    private GeminiGeradorIdeias gerador() {
        return new GeminiGeradorIdeias("chave-teste", PRINCIPAL,
                "http://127.0.0.1:" + servidor.getAddress().getPort() + "/", Duration.ofMillis(700));
    }

    private static Pessoa pessoa() {
        return new Pessoa(1, new DadosPessoa("Ana", 30, Vinculo.AMIGO, List.of("café"), List.of(), Ocasiao.NATAL,
                new FaixaOrcamento(new BigDecimal("200"))));
    }

    @Test
    void usaOModeloPrincipalQuandoEleResponde() {
        List<IdeiaPresente> ideias = gerador().gerarIdeias(pessoa(), 6);

        assertThat(ideias).extracting(IdeiaPresente::nome).containsExactly("Caneca");
        assertThat(modelosChamados).containsExactly(PRINCIPAL);
    }

    @Test
    void quandoOModeloPrincipalNaoRespondeATempoUsaOReserva() {
        comportamento.put(PRINCIPAL, "travar");

        List<IdeiaPresente> ideias = gerador().gerarIdeias(pessoa(), 6);

        assertThat(ideias).extracting(IdeiaPresente::nome).containsExactly("Caneca");
        assertThat(modelosChamados).containsExactly(PRINCIPAL, RESERVA);
    }

    @Test
    void quandoOModeloPrincipalEstaSobrecarregadoUsaOReserva() {
        comportamento.put(PRINCIPAL, 503);

        assertThat(gerador().gerarIdeias(pessoa(), 6)).hasSize(1);
        assertThat(modelosChamados).containsExactly(PRINCIPAL, RESERVA);
    }

    @Test
    void quandoNenhumModeloRespondeATempoAvisaQueDemorouENaoCulpaAInternet() {
        comportamento.put(PRINCIPAL, "travar");
        comportamento.put(RESERVA, "travar");

        assertThatThrownBy(() -> gerador().gerarIdeias(pessoa(), 6))
                .isInstanceOf(IntegracaoIaException.class)
                .hasMessage("O Gemini demorou demais para responder e parece sobrecarregado. Tente novamente em instantes.");
    }

    @Test
    void quandoUmTravaEOOutroEstaSobrecarregadoAvisaDaSobrecarga() {
        comportamento.put(PRINCIPAL, "travar");
        comportamento.put(RESERVA, 503);

        assertThatThrownBy(() -> gerador().gerarIdeias(pessoa(), 6))
                .isInstanceOf(IntegracaoIaException.class)
                .hasMessage("O Gemini está sobrecarregado no momento. Tente novamente em instantes.");
    }

    @Test
    void semConexaoAvisaParaVerificarAInternet() {
        int portaFechada = servidor.getAddress().getPort();
        servidor.stop(0);
        GeminiGeradorIdeias gerador = new GeminiGeradorIdeias("chave-teste", PRINCIPAL,
                "http://127.0.0.1:" + portaFechada + "/", Duration.ofMillis(700));

        assertThatThrownBy(() -> gerador.gerarIdeias(pessoa(), 6))
                .isInstanceOf(IntegracaoIaException.class)
                .hasMessage("Não foi possível conectar ao Gemini. Verifique sua internet.");
    }
}
