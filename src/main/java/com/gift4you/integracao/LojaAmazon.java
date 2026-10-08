package com.gift4you.integracao;

import com.gift4you.model.ProdutoLoja;
import com.gift4you.service.LojaOnline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Procura produtos lendo a página de busca da Amazon Brasil.
 * Atenção: os Termos de Uso da Amazon não permitem leitura automática do site, e a Amazon pode
 * bloquear as consultas. Quando a busca falha, as ideias usam o link de busca.
 */
public class LojaAmazon implements LojaOnline {

    private static final Logger LOG = LoggerFactory.getLogger(LojaAmazon.class);

    private static final String ENDERECO_BUSCA = "https://www.amazon.com.br/s?k=";
    private static final Pattern LINK_PRODUTO = Pattern.compile(
            "^" + Pattern.quote(LeitorBuscaAmazon.ENDERECO_PRODUTO) + "[A-Z0-9]{10}$");
    private static final Pattern IMAGEM_DA_LOJA = Pattern.compile("^https://m\\.media-amazon\\.com/images/[^\\s\"'<>]+$");
    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(10);
    private static final int TAMANHO_MAXIMO_PAGINA = 4 * 1024 * 1024;

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(TEMPO_LIMITE)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final LeitorBuscaAmazon leitor;

    public LojaAmazon(LeitorBuscaAmazon leitor) {
        this.leitor = leitor;
    }

    @Override
    public String linkBusca(String nomeProduto) {
        return ENDERECO_BUSCA + URLEncoder.encode(nomeProduto.trim(), StandardCharsets.UTF_8);
    }

    @Override
    public Optional<ProdutoLoja> encontrarProduto(String nomeProduto) {
        try {
            HttpResponse<InputStream> resposta = cliente.send(
                    RequisicaoNavegador.paginaHtml(URI.create(linkBusca(nomeProduto)), TEMPO_LIMITE),
                    HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream corpo = resposta.body()) {
                if (resposta.statusCode() != 200) {
                    LOG.info("Busca na Amazon por \"{}\" respondeu {}", nomeProduto, resposta.statusCode());
                    return Optional.empty();
                }
                String html = new String(corpo.readNBytes(TAMANHO_MAXIMO_PAGINA), StandardCharsets.UTF_8);
                Optional<ProdutoLoja> produto = leitor.primeiroProduto(html);
                if (produto.isEmpty()) {
                    LOG.info("Nenhum produto lido na busca da Amazon por \"{}\" (página mudou ou foi bloqueada)",
                            nomeProduto);
                }
                return produto;
            }
        } catch (IOException e) {
            LOG.info("Não foi possível consultar a Amazon por \"{}\": {}", nomeProduto, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    @Override
    public boolean ehLinkDeProduto(String link) {
        return link != null && LINK_PRODUTO.matcher(link).matches();
    }

    @Override
    public boolean ehImagemDaLoja(String imagemUrl) {
        return imagemUrl != null && IMAGEM_DA_LOJA.matcher(imagemUrl).matches();
    }
}
