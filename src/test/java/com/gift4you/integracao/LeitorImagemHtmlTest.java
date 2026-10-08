package com.gift4you.integracao;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LeitorImagemHtmlTest {

    private static final URI PAGINA = URI.create("https://www.loja.com.br/produtos/fone-123");

    private final LeitorImagemHtml leitor = new LeitorImagemHtml();

    private Optional<String> imagem(String html) {
        return leitor.encontrarImagem(html, PAGINA);
    }

    @Test
    void usaAImagemOpenGraph() {
        String html = """
                <html><head>
                <meta name="description" content="Fone">
                <meta property="og:image" content="https://img.loja.com.br/fone.jpg">
                </head></html>""";

        assertThat(imagem(html)).contains("https://img.loja.com.br/fone.jpg");
    }

    @Test
    void prefereAVersaoSeguraEAceitaAtributosEmQualquerOrdemEComAspasSimples() {
        String html = """
                <meta content='http://img.loja.com.br/a.jpg' property='og:image'/>
                <META CONTENT="https://img.loja.com.br/a-segura.jpg" PROPERTY="og:image:secure_url">""";

        assertThat(imagem(html)).contains("https://img.loja.com.br/a-segura.jpg");
    }

    @Test
    void usaTwitterCardQuandoNaoHaOpenGraph() {
        assertThat(imagem("<meta name=\"twitter:image\" content=\"https://img.loja.com.br/t.png\">"))
                .contains("https://img.loja.com.br/t.png");
    }

    @Test
    void usaLinkImageSrc() {
        assertThat(imagem("<link rel=\"image_src\" href=\"https://img.loja.com.br/l.png\">"))
                .contains("https://img.loja.com.br/l.png");
    }

    @Test
    void usaOsDadosEstruturadosDoProduto() {
        String html = """
                <script type="application/ld+json">
                {"@context":"https://schema.org","@graph":[
                  {"@type":"BreadcrumbList","itemListElement":[]},
                  {"@type":"Product","name":"Fone","image":["https://img.loja.com.br/p1.jpg","https://img.loja.com.br/p2.jpg"]}
                ]}
                </script>""";

        assertThat(imagem(html)).contains("https://img.loja.com.br/p1.jpg");
    }

    @Test
    void aceitaImagemComoObjetoNoJsonLd() {
        String html = "<script type='application/ld+json'>{\"@type\":\"Product\",\"image\":{\"url\":\"/img/o.jpg\"}}</script>";

        assertThat(imagem(html)).contains("https://www.loja.com.br/img/o.jpg");
    }

    @Test
    void ignoraJsonLdMalformadoESegueParaOProximo() {
        String html = """
                <script type="application/ld+json">{ quebrado </script>
                <script type="application/ld+json">{"image":"https://img.loja.com.br/ok.jpg"}</script>""";

        assertThat(imagem(html)).contains("https://img.loja.com.br/ok.jpg");
    }

    @Test
    void resolveEnderecosRelativosEDecodificaEntidades() {
        assertThat(imagem("<meta property=\"og:image\" content=\"/fotos/fone.jpg?w=500&amp;h=500\">"))
                .contains("https://www.loja.com.br/fotos/fone.jpg?w=500&h=500");
        assertThat(imagem("<meta property=\"og:image\" content=\"//cdn.loja.com.br/f.jpg\">"))
                .contains("https://cdn.loja.com.br/f.jpg");
        assertThat(imagem("<meta property=\"og:image\" content=\"https:&#x2F;&#x2F;cdn.loja.com.br&#47;e.jpg\">"))
                .contains("https://cdn.loja.com.br/e.jpg");
    }

    @Test
    void recusaEnderecosQueNaoSaoHttp() {
        assertThat(imagem("<meta property=\"og:image\" content=\"javascript:alert(1)\">")).isEmpty();
        assertThat(imagem("<meta property=\"og:image\" content=\"data:image/png;base64,AAAA\">")).isEmpty();
    }

    @Test
    void semImagemDeclaradaRetornaVazio() {
        assertThat(imagem("<html><head><title>Loja</title><meta property=\"og:image\" content=\"\"></head></html>"))
                .isEmpty();
    }
}
