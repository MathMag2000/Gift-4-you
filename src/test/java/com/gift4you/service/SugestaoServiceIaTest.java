package com.gift4you.service;

import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Pessoa;
import com.gift4you.model.ProdutoLoja;
import com.gift4you.model.RegistroHistorico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SugestaoServiceIaTest {

    private static final ProdutoLoja PRENSA_NA_LOJA = new ProdutoLoja("Prensa Francesa Mimo 600ml",
            "https://www.amazon.com.br/dp/B07WXJ8GQP", "https://m.media-amazon.com/images/I/61abc.jpg");

    private Cenario cenario;
    private Pessoa ana;

    @BeforeEach
    void preparar() {
        cenario = new Cenario();
        ana = cenario.cadastrarPessoa("Ana", List.of("café"), List.of("perfume"));
    }

    @Test
    void ideiaEncontradaNaLojaGanhaOLinkEAFotoDoProduto() {
        cenario.ideiasDaIa.add(Cenario.ideia("Prensa francesa", "120"));
        cenario.produtosNaLoja.put("Prensa francesa", PRENSA_NA_LOJA);

        IdeiaPresente ideia = cenario.sugestoes.gerarIdeiasComIa(ana.getId()).ideias().getFirst();

        assertThat(ideia.linkCompra()).isEqualTo("https://www.amazon.com.br/dp/B07WXJ8GQP");
        assertThat(ideia.imagemUrl()).isEqualTo("https://m.media-amazon.com/images/I/61abc.jpg");
        assertThat(ideia.nome()).as("o nome continua o da ideia").isEqualTo("Prensa francesa");
    }

    @Test
    void ideiaNaoEncontradaFicaComOLinkDeBusca() {
        cenario.ideiasDaIa.add(Cenario.ideia("Prensa francesa", "120"));

        IdeiaPresente ideia = cenario.sugestoes.gerarIdeiasComIa(ana.getId()).ideias().getFirst();

        assertThat(ideia.linkCompra()).isEqualTo("https://www.amazon.com.br/s?k=Prensa+francesa");
        assertThat(ideia.imagemUrl()).isNull();
    }

    @Test
    void produtoDaLojaRelacionadoAoQueAPessoaNaoGostaNaoEUsado() {
        cenario.ideiasDaIa.add(Cenario.ideia("Kit presente", "120"));
        cenario.produtosNaLoja.put("Kit presente", new ProdutoLoja("Kit Perfume e Hidratante",
                "https://www.amazon.com.br/dp/B000000001", "https://m.media-amazon.com/images/I/p.jpg"));

        IdeiaPresente ideia = cenario.sugestoes.gerarIdeiasComIa(ana.getId()).ideias().getFirst();

        assertThat(ideia.linkCompra()).isEqualTo("https://www.amazon.com.br/s?k=Kit+presente");
        assertThat(ideia.imagemUrl()).isNull();
    }

    @Test
    void procuraTodasAsIdeiasEMantemAOrdem() {
        cenario.ideiasDaIa.addAll(List.of(Cenario.ideia("Prensa francesa", "120"),
                Cenario.ideia("Caneca", "60"), Cenario.ideia("Moedor de café", "150")));
        cenario.produtosNaLoja.put("Prensa francesa", PRENSA_NA_LOJA);
        cenario.produtosNaLoja.put("Moedor de café", new ProdutoLoja("Moedor Manual",
                "https://www.amazon.com.br/dp/B000000002", null));

        List<IdeiaPresente> ideias = cenario.sugestoes.gerarIdeiasComIa(ana.getId()).ideias();

        assertThat(ideias).extracting(IdeiaPresente::nome).containsExactly("Prensa francesa", "Caneca", "Moedor de café");
        assertThat(ideias).extracting(IdeiaPresente::linkCompra).containsExactly(
                "https://www.amazon.com.br/dp/B07WXJ8GQP",
                "https://www.amazon.com.br/s?k=Caneca",
                "https://www.amazon.com.br/dp/B000000002");
    }

    @Test
    void historicoGuardaOLinkEAFotoDoProdutoEncontrado() {
        cenario.ideiasDaIa.add(Cenario.ideia("Prensa francesa", "120"));
        cenario.produtosNaLoja.put("Prensa francesa", PRENSA_NA_LOJA);

        cenario.sugestoes.gerarIdeiasComIa(ana.getId());

        RegistroHistorico registro = cenario.historico.listar(ana.getId()).getFirst();
        assertThat(registro.getItens().getFirst().linkCompra()).isEqualTo("https://www.amazon.com.br/dp/B07WXJ8GQP");
        assertThat(registro.getItens().getFirst().imagemUrl()).isEqualTo("https://m.media-amazon.com/images/I/61abc.jpg");
    }
}
