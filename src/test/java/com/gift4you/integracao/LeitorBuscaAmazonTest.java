package com.gift4you.integracao;

import com.gift4you.model.ProdutoLoja;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LeitorBuscaAmazonTest {

    private final LeitorBuscaAmazon leitor = new LeitorBuscaAmazon();

    /** Trechos no formato da página de busca da Amazon: um anúncio patrocinado e dois resultados normais. */
    private static final String PAGINA = """
            <div role="listitem" data-asin="B0GZSD9F5Q" data-index="2" data-component-type="s-search-result"
                 class="sg-col-4-of-24 s-result-item s-asin AdHolder sg-col s-widget-spacing-small">
              <a href="/sspa/click?spc=abc"><span class="s-sponsored-label-text">Patrocinado</span></a>
              <img class="s-image" src="https://m.media-amazon.com/images/I/41Lsfolmg2L._AC_UL320_.jpg">
              <h2 aria-label="Anúncio patrocinado – Filtro de Café Portátil"><span>Filtro</span></h2>
            </div>
            <div role="listitem" data-asin="B094N9VPPN" data-index="3" data-component-type="s-search-result"
                 class="sg-col-4-of-24 s-result-item s-asin sg-col s-widget-spacing-small">
              <img class="s-image" src="https://m.media-amazon.com/images/I/411nQMjc4qS._AC_UL320_.jpg" alt="">
              <h2 aria-label="Filtro de Café Dobrável Portátil &amp; Reutilizável"><span>Filtro</span></h2>
              <span class="a-price"><span class="a-offscreen">R$ 14,07</span></span>
              <span class="a-price a-text-price"><span class="a-offscreen">R$ 19,90</span></span>
            </div>
            <div role="listitem" data-asin="B07WXJ8GQP" data-index="4" data-component-type="s-search-result" class="s-result-item">
              <img src="https://m.media-amazon.com/images/I/61abc.png" class="s-image">
              <h2 class="a-size-base"><span class="a-text-normal">Prensa Francesa 600ml</span></h2>
              <span class="a-offscreen">R$&nbsp;1.299,90</span>
            </div>""";

    @Test
    void ignoraAnunciosEDevolveOPrimeiroResultadoNormalComOPrecoAtual() {
        assertThat(leitor.produtos(PAGINA).getFirst()).isEqualTo(new ProdutoLoja(
                "Filtro de Café Dobrável Portátil & Reutilizável",
                "https://www.amazon.com.br/dp/B094N9VPPN",
                "https://m.media-amazon.com/images/I/411nQMjc4qS.jpg",
                new BigDecimal("14.07")));
    }

    @Test
    void leTodosOsResultadosNaOrdemDaBusca() {
        assertThat(leitor.produtos(PAGINA)).extracting(ProdutoLoja::link).containsExactly(
                "https://www.amazon.com.br/dp/B094N9VPPN",
                "https://www.amazon.com.br/dp/B07WXJ8GQP");
    }

    @Test
    void aceitaTituloDentroDoSpanEAtributosEmOutraOrdem() {
        ProdutoLoja prensa = leitor.produtos(PAGINA).get(1);

        assertThat(prensa.titulo()).isEqualTo("Prensa Francesa 600ml");
        assertThat(prensa.imagemUrl()).isEqualTo("https://m.media-amazon.com/images/I/61abc.png");
        assertThat(prensa.preco()).isEqualByComparingTo("1299.90");
    }

    @Test
    void paginaSemResultadosOuBloqueadaNaoEncontraNada() {
        String captcha = "<html><form action=\"/errors/validateCaptcha\"><img src=\"captcha.jpg\"></form></html>";

        assertThat(leitor.produtos(captcha)).isEmpty();
        assertThat(leitor.produtos("")).isEmpty();
    }
}
