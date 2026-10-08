package com.gift4you.service;

import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Pessoa;
import com.gift4you.model.ProdutoLoja;
import com.gift4you.model.RegistroHistorico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SugestaoServiceIaTest {

    private static final ProdutoLoja PRENSA_NA_LOJA = produto("Prensa Francesa Mimo 600ml", "B07WXJ8GQP", "89.90");

    private Cenario cenario;
    private Pessoa ana;

    @BeforeEach
    void preparar() {
        cenario = new Cenario();
        ana = cenario.cadastrarPessoa("Ana", List.of("café"), List.of("perfume"));
    }

    private static ProdutoLoja produto(String titulo, String asin, String preco) {
        return new ProdutoLoja(titulo, "https://www.amazon.com.br/dp/" + asin,
                "https://m.media-amazon.com/images/I/" + asin + ".jpg", preco == null ? null : new BigDecimal(preco));
    }

    @Test
    void ideiaEncontradaNaLojaAssumeNomePrecoLinkEFotoDoProduto() {
        cenario.ideiasDaIa.add(Cenario.ideia("Prensa francesa", "120"));
        cenario.produtosNaLoja.put("Prensa francesa", List.of(PRENSA_NA_LOJA));

        IdeiaPresente ideia = cenario.sugestoes.gerarIdeiasComIa(ana.getId()).ideias().getFirst();

        assertThat(ideia.nome()).isEqualTo("Prensa Francesa Mimo 600ml");
        assertThat(ideia.precoEstimado()).isEqualByComparingTo("89.90");
        assertThat(ideia.linkCompra()).isEqualTo("https://www.amazon.com.br/dp/B07WXJ8GQP");
        assertThat(ideia.imagemUrl()).isEqualTo("https://m.media-amazon.com/images/I/B07WXJ8GQP.jpg");
        assertThat(ideia.motivo()).isEqualTo("Motivo de Prensa francesa");
    }

    @Test
    void ideiaNaoEncontradaFicaComOLinkDeBuscaEOPrecoEstimado() {
        cenario.ideiasDaIa.add(Cenario.ideia("Prensa francesa", "120"));

        IdeiaPresente ideia = cenario.sugestoes.gerarIdeiasComIa(ana.getId()).ideias().getFirst();

        assertThat(ideia.nome()).isEqualTo("Prensa francesa");
        assertThat(ideia.precoEstimado()).isEqualByComparingTo("120");
        assertThat(ideia.linkCompra()).isEqualTo("https://www.amazon.com.br/s?k=Prensa+francesa");
        assertThat(ideia.imagemUrl()).isNull();
    }

    @Test
    void pulaProdutosAcimaDoOrcamentoOuSemPreco() {
        cenario.ideiasDaIa.add(Cenario.ideia("Cafeteira", "150"));
        cenario.produtosNaLoja.put("Cafeteira", List.of(
                produto("Cafeteira Expresso Profissional", "B000000001", "1299.00"),
                produto("Cafeteira sem preço", "B000000002", null),
                produto("Cafeteira Italiana", "B000000003", "149.90")));

        IdeiaPresente ideia = cenario.sugestoes.gerarIdeiasComIa(ana.getId()).ideias().getFirst();

        assertThat(ideia.nome()).isEqualTo("Cafeteira Italiana");
        assertThat(ideia.precoEstimado()).isEqualByComparingTo("149.90");
    }

    @Test
    void produtoDaLojaRelacionadoAoQueAPessoaNaoGostaNaoEUsado() {
        cenario.ideiasDaIa.add(Cenario.ideia("Kit presente", "120"));
        cenario.produtosNaLoja.put("Kit presente", List.of(produto("Kit Perfume e Hidratante", "B000000001", "99")));

        IdeiaPresente ideia = cenario.sugestoes.gerarIdeiasComIa(ana.getId()).ideias().getFirst();

        assertThat(ideia.nome()).isEqualTo("Kit presente");
        assertThat(ideia.linkCompra()).isEqualTo("https://www.amazon.com.br/s?k=Kit+presente");
        assertThat(ideia.imagemUrl()).isNull();
    }

    @Test
    void procuraTodasAsIdeiasEMantemAOrdem() {
        cenario.ideiasDaIa.addAll(List.of(Cenario.ideia("Prensa francesa", "120"),
                Cenario.ideia("Caneca", "60"), Cenario.ideia("Moedor de café", "150")));
        cenario.produtosNaLoja.put("Prensa francesa", List.of(PRENSA_NA_LOJA));
        cenario.produtosNaLoja.put("Moedor de café", List.of(produto("Moedor Manual", "B000000002", "75")));

        List<IdeiaPresente> ideias = cenario.sugestoes.gerarIdeiasComIa(ana.getId()).ideias();

        assertThat(ideias).extracting(IdeiaPresente::nome)
                .containsExactly("Prensa Francesa Mimo 600ml", "Caneca", "Moedor Manual");
        assertThat(ideias).extracting(IdeiaPresente::linkCompra).containsExactly(
                "https://www.amazon.com.br/dp/B07WXJ8GQP",
                "https://www.amazon.com.br/s?k=Caneca",
                "https://www.amazon.com.br/dp/B000000002");
    }

    @Test
    void historicoGuardaOsDadosDoProdutoEncontrado() {
        cenario.ideiasDaIa.add(Cenario.ideia("Prensa francesa", "120"));
        cenario.produtosNaLoja.put("Prensa francesa", List.of(PRENSA_NA_LOJA));

        cenario.sugestoes.gerarIdeiasComIa(ana.getId());

        RegistroHistorico registro = cenario.historico.listar(ana.getId()).getFirst();
        assertThat(registro.getItens().getFirst()).satisfies(item -> {
            assertThat(item.nome()).isEqualTo("Prensa Francesa Mimo 600ml");
            assertThat(item.preco()).isEqualByComparingTo("89.90");
            assertThat(item.linkCompra()).isEqualTo("https://www.amazon.com.br/dp/B07WXJ8GQP");
            assertThat(item.imagemUrl()).isEqualTo("https://m.media-amazon.com/images/I/B07WXJ8GQP.jpg");
        });
    }
}
