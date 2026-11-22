package com.gift4you.integracao;

import com.gift4you.model.ProdutoLoja;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Busca na Amazon por um intermediário (serviço de leitura de páginas), simulado por um servidor local.
 */
class LojaAmazonIntermediarioTest {

    private static final String PAGINA = """
            <div role="listitem" data-asin="B094N9VPPN" data-index="3" data-component-type="s-search-result"
                 class="sg-col-4-of-24 s-result-item s-asin sg-col s-widget-spacing-small">
              <img class="s-image" src="https://m.media-amazon.com/images/I/411nQMjc4qS._AC_UL320_.jpg" alt="">
              <h2 aria-label="Caneca de Cerâmica 350ml"><span>Caneca</span></h2>
              <span class="a-price"><span class="a-offscreen">R$ 49,90</span></span>
            </div>""";

    private final List<String> consultas = new CopyOnWriteArrayList<>();
    private HttpServer servidor;
    private int statusResposta = 200;

    @BeforeEach
    void iniciar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", troca -> {
            consultas.add(troca.getRequestURI().getRawQuery());
            byte[] corpo = PAGINA.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(statusResposta, corpo.length);
            troca.getResponseBody().write(corpo);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void parar() {
        servidor.stop(0);
    }

    private LojaAmazon lojaPeloIntermediario() {
        return new LojaAmazon(new LeitorBuscaAmazon(),
                "http://127.0.0.1:" + servidor.getAddress().getPort() + "/?api_key=chave-secreta&url={url}",
                Duration.ofSeconds(5));
    }

    @Test
    void enviaAoIntermediarioOEnderecoDaBuscaCodificadoELeOsProdutos() {
        List<ProdutoLoja> produtos = lojaPeloIntermediario().encontrarProdutos("Caneca de cerâmica");

        assertThat(produtos).singleElement().satisfies(produto -> {
            assertThat(produto.titulo()).isEqualTo("Caneca de Cerâmica 350ml");
            assertThat(produto.link()).isEqualTo("https://www.amazon.com.br/dp/B094N9VPPN");
            assertThat(produto.preco()).isEqualByComparingTo("49.90");
        });
        String consulta = consultas.getFirst();
        assertThat(consulta).startsWith("api_key=chave-secreta&url=https%3A%2F%2Fwww.amazon.com.br%2Fs%3Fk%3D");
        assertThat(URLDecoder.decode(consulta.substring(consulta.indexOf("url=") + 4), StandardCharsets.UTF_8))
                .isEqualTo("https://www.amazon.com.br/s?k=Caneca+de+cer%C3%A2mica");
    }

    @Test
    void falhaDoIntermediarioNaoImpedeAsIdeias() {
        statusResposta = 500;

        assertThat(lojaPeloIntermediario().encontrarProdutos("Caneca")).isEmpty();
    }

    @Test
    void semIntermediarioConsultaAAmazonDiretamente() {
        LojaAmazon loja = new LojaAmazon(new LeitorBuscaAmazon(), "  ");

        assertThat(loja.enderecoConsulta("Caneca")).hasToString("https://www.amazon.com.br/s?k=Caneca");
    }

    @Test
    void recusaIntermediarioSemHttpsOuSemOMarcador() {
        assertThatThrownBy(() -> new LojaAmazon(new LeitorBuscaAmazon(), "http://servico.com/?url={url}"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("AMAZON_PROXY_URL");
        assertThatThrownBy(() -> new LojaAmazon(new LeitorBuscaAmazon(), "https://servico.com/?api_key=x"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("{url}");
    }

    @Test
    void aceitaOFormatoDosServicosConhecidos() {
        LojaAmazon loja = new LojaAmazon(new LeitorBuscaAmazon(),
                "https://api.scraperapi.com/?api_key=abc&country_code=br&url={url}");

        assertThat(loja.enderecoConsulta("Fone").toString()).isEqualTo(
                "https://api.scraperapi.com/?api_key=abc&country_code=br&url=https%3A%2F%2Fwww.amazon.com.br%2Fs%3Fk%3DFone");
    }
}
