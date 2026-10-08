package com.gift4you.integracao;

import com.gift4you.service.ExtratorImagemProduto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Baixa a página de compra e extrai dela a imagem do produto.
 * Falhas (loja bloqueando robôs, página fora do ar) não impedem o cadastro: apenas não há imagem.
 */
public class ExtratorImagemPaginaWeb implements ExtratorImagemProduto {

    private static final Logger LOG = LoggerFactory.getLogger(ExtratorImagemPaginaWeb.class);

    private static final int MAXIMO_REDIRECIONAMENTOS = 5;
    private static final int TAMANHO_MAXIMO_PAGINA = 2 * 1024 * 1024;
    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(10);
    private static final Pattern CHARSET = Pattern.compile("charset=([\\w-]+)", Pattern.CASE_INSENSITIVE);

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(TEMPO_LIMITE)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final ProtecaoEnderecoInterno protecao;
    private final LeitorImagemHtml leitor;

    public ExtratorImagemPaginaWeb(ProtecaoEnderecoInterno protecao, LeitorImagemHtml leitor) {
        this.protecao = protecao;
        this.leitor = leitor;
    }

    @Override
    public Optional<String> extrair(String linkCompra) {
        try {
            URI endereco = URI.create(linkCompra);
            for (int tentativa = 0; tentativa <= MAXIMO_REDIRECIONAMENTOS; tentativa++) {
                if (!protecao.permitido(endereco)) {
                    LOG.warn("Endereço recusado ao buscar imagem: {}", endereco.getHost());
                    return Optional.empty();
                }
                HttpResponse<InputStream> resposta = cliente.send(
                        RequisicaoNavegador.paginaHtml(endereco, TEMPO_LIMITE), HttpResponse.BodyHandlers.ofInputStream());
                Optional<String> destino = resposta.headers().firstValue("Location");
                if (resposta.statusCode() / 100 == 3 && destino.isPresent()) {
                    resposta.body().close();
                    endereco = endereco.resolve(destino.get());
                    continue;
                }
                return lerImagem(endereco, resposta);
            }
            LOG.info("Muitos redirecionamentos ao buscar imagem de {}", linkCompra);
        } catch (IOException | IllegalArgumentException e) {
            LOG.info("Não foi possível ler a página {}: {}", linkCompra, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    private Optional<String> lerImagem(URI endereco, HttpResponse<InputStream> resposta) throws IOException {
        String tipo = resposta.headers().firstValue("Content-Type").orElse("");
        try (InputStream corpo = resposta.body()) {
            if (resposta.statusCode() != 200 || !tipo.toLowerCase().contains("html")) {
                LOG.info("Página {} respondeu {} ({}); sem imagem", endereco.getHost(), resposta.statusCode(), tipo);
                return Optional.empty();
            }
            String html = new String(corpo.readNBytes(TAMANHO_MAXIMO_PAGINA), codificacao(tipo));
            return leitor.encontrarImagem(html, endereco);
        }
    }

    private Charset codificacao(String tipo) {
        Matcher charset = CHARSET.matcher(tipo);
        try {
            return charset.find() ? Charset.forName(charset.group(1)) : StandardCharsets.UTF_8;
        } catch (IllegalArgumentException e) {
            return StandardCharsets.UTF_8;
        }
    }
}
