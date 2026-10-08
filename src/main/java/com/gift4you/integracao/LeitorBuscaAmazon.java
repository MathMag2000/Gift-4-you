package com.gift4you.integracao;

import com.gift4you.model.ProdutoLoja;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê a página de resultados de busca da Amazon e extrai os produtos, ignorando anúncios patrocinados.
 * Depende da estrutura atual da página da Amazon: se ela mudar, nenhum produto é encontrado
 * e o sistema volta a usar o link de busca.
 */
public class LeitorBuscaAmazon {

    static final String ENDERECO_PRODUTO = "https://www.amazon.com.br/dp/";

    private static final Pattern INICIO_RESULTADO = Pattern.compile(
            "<div[^>]*data-component-type=\"s-search-result\"[^>]*>");
    private static final Pattern ASIN = Pattern.compile("data-asin=\"([A-Z0-9]{10})\"");
    private static final Pattern PATROCINADO = Pattern.compile("AdHolder|s-sponsored-label|/sspa/");
    private static final Pattern TITULO = Pattern.compile("<h2[^>]*aria-label=\"([^\"]+)\"|<h2[^>]*>\\s*<span[^>]*>([^<]+)<");
    private static final Pattern IMAGEM = Pattern.compile(
            "<img[^>]*class=\"s-image\"[^>]*src=\"([^\"]+)\"|<img[^>]*src=\"([^\"]+)\"[^>]*class=\"s-image\"");
    /** Sufixo de tamanho das imagens da Amazon (ex.: ._AC_UL320_); sem ele, vem a imagem original. */
    private static final Pattern TAMANHO_IMAGEM = Pattern.compile("\\._[^/]*_\\.(jpg|jpeg|png|webp)$");
    private static final int TAMANHO_MAXIMO_BLOCO = 60_000;

    public Optional<ProdutoLoja> primeiroProduto(String html) {
        return produtos(html).stream().findFirst();
    }

    /** Produtos da página, na ordem da busca, sem os anúncios patrocinados. */
    public List<ProdutoLoja> produtos(String html) {
        List<int[]> posicoes = new ArrayList<>();
        Matcher inicio = INICIO_RESULTADO.matcher(html);
        while (inicio.find()) {
            posicoes.add(new int[]{inicio.start(), inicio.end()});
        }
        List<ProdutoLoja> produtos = new ArrayList<>();
        for (int i = 0; i < posicoes.size(); i++) {
            int comeco = posicoes.get(i)[0];
            int fim = i + 1 < posicoes.size()
                    ? posicoes.get(i + 1)[0]
                    : Math.min(html.length(), comeco + TAMANHO_MAXIMO_BLOCO);
            String tag = html.substring(comeco, posicoes.get(i)[1]);
            lerProduto(tag, html.substring(comeco, fim)).ifPresent(produtos::add);
        }
        return produtos;
    }

    private Optional<ProdutoLoja> lerProduto(String tag, String bloco) {
        Matcher asin = ASIN.matcher(tag);
        if (!asin.find() || PATROCINADO.matcher(bloco).find()) {
            return Optional.empty();
        }
        String titulo = primeiroGrupo(TITULO.matcher(bloco)).map(this::decodificar).orElse("");
        String imagem = primeiroGrupo(IMAGEM.matcher(bloco))
                .map(endereco -> TAMANHO_IMAGEM.matcher(endereco).replaceFirst(".$1"))
                .orElse(null);
        return Optional.of(new ProdutoLoja(titulo, ENDERECO_PRODUTO + asin.group(1), imagem));
    }

    private Optional<String> primeiroGrupo(Matcher matcher) {
        if (!matcher.find()) {
            return Optional.empty();
        }
        for (int grupo = 1; grupo <= matcher.groupCount(); grupo++) {
            if (matcher.group(grupo) != null) {
                return Optional.of(matcher.group(grupo).trim());
            }
        }
        return Optional.empty();
    }

    private String decodificar(String texto) {
        return texto.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">");
    }
}
