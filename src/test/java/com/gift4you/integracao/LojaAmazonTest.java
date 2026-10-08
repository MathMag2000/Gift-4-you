package com.gift4you.integracao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class LojaAmazonTest {

    private final LojaAmazon loja = new LojaAmazon(new LeitorBuscaAmazon());

    @Test
    void montaOLinkDeBuscaCodificandoAcentosESimbolos() {
        assertThat(loja.linkBusca("  Caneca térmica 500ml & tampa #2 ")).isEqualTo(
                "https://www.amazon.com.br/s?k=Caneca+t%C3%A9rmica+500ml+%26+tampa+%232");
    }

    @Test
    void reconheceLinkDeProduto() {
        assertThat(loja.ehLinkDeProduto("https://www.amazon.com.br/dp/B094N9VPPN")).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "https://www.amazon.com.br/s?k=fone",
            "https://www.amazon.com.br/dp/B094N9VPPN?tag=x", "https://amazon.golpe.com/dp/B094N9VPPN",
            "http://www.amazon.com.br/dp/B094N9VPPN", "https://www.amazon.com.br/dp/b094n9vppn"})
    void recusaOutrosLinks(String link) {
        assertThat(loja.ehLinkDeProduto(link)).isFalse();
        assertThat(loja.ehLinkDeProduto(null)).isFalse();
    }

    @Test
    void reconheceImagensDaAmazon() {
        assertThat(loja.ehImagemDaLoja("https://m.media-amazon.com/images/I/411nQMjc4qS.jpg")).isTrue();
        assertThat(loja.ehImagemDaLoja("https://golpe.com/images/I/a.jpg")).isFalse();
        assertThat(loja.ehImagemDaLoja("https://m.media-amazon.com/images/I/a.jpg\" onerror=\"x")).isFalse();
        assertThat(loja.ehImagemDaLoja(null)).isFalse();
    }
}
