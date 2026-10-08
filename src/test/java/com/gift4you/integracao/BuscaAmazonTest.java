package com.gift4you.integracao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuscaAmazonTest {

    private final BuscaAmazon busca = new BuscaAmazon();

    @Test
    void montaABuscaDoProdutoNaAmazon() {
        assertThat(busca.linkPara("Prensa francesa")).isEqualTo("https://www.amazon.com.br/s?k=Prensa+francesa");
    }

    @Test
    void codificaAcentosESimbolosParaNaoQuebrarOLink() {
        assertThat(busca.linkPara("  Caneca térmica 500ml & tampa #2 ")).isEqualTo(
                "https://www.amazon.com.br/s?k=Caneca+t%C3%A9rmica+500ml+%26+tampa+%232");
    }
}
