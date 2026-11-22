package com.gift4you.service;

import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Pessoa;
import com.gift4you.model.ProdutoLoja;
import com.gift4you.model.ResultadoIdeias;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Uma nova geração de ideias com IA não repete o que já foi sugerido para a mesma pessoa. */
class IdeiasSemRepeticaoTest {

    private Cenario cenario;
    private Pessoa ana;
    /** O que o gerador recebeu como ideias já sugeridas, a cada pedido. */
    private final List<List<String>> jaSugeridasRecebidas = new ArrayList<>();
    private SugestaoService sugestoes;

    @BeforeEach
    void preparar() {
        cenario = new Cenario();
        ana = cenario.cadastrarPessoa("Ana", List.of("café"), List.of());
        GeradorIdeias gerador = new GeradorIdeias() {
            @Override
            public List<IdeiaPresente> gerarIdeias(Pessoa pessoa, int quantidade) {
                return gerarIdeias(pessoa, quantidade, List.of());
            }

            @Override
            public List<IdeiaPresente> gerarIdeias(Pessoa pessoa, int quantidade, List<String> jaSugeridas) {
                jaSugeridasRecebidas.add(jaSugeridas);
                return List.copyOf(cenario.ideiasDaIa);
            }
        };
        sugestoes = new SugestaoService(cenario.pessoas, cenario.presentes, new ComparadorTermos(), gerador,
                cenario.historico, cenario.loja, new LimiteDeTaxa(100, Duration.ofHours(1), cenario.relogio));
    }

    private ResultadoIdeias gerar(Pessoa pessoa, String... nomes) {
        cenario.ideiasDaIa.clear();
        for (String nome : nomes) {
            cenario.ideiasDaIa.add(Cenario.ideia(nome, "50"));
        }
        return sugestoes.gerarIdeiasComIa(Cenario.USUARIO, pessoa.getId());
    }

    private static List<String> nomes(ResultadoIdeias resultado) {
        return resultado.ideias().stream().map(IdeiaPresente::nome).toList();
    }

    @Test
    void naoRepeteIdeiasJaSugeridasMesmoComAcentoOuMaiusculasDiferentes() {
        gerar(ana, "Caneca de cerâmica", "Prensa francesa");

        ResultadoIdeias segunda = gerar(ana, "CANECA de ceramica!", "Livro de receitas", "prensa  francesa");

        assertThat(nomes(segunda)).containsExactly("Livro de receitas");
        assertThat(segunda.repetidas()).isEqualTo(2);
        assertThat(cenario.historico.listar(Cenario.USUARIO, ana.getId()).getFirst().getItens())
                .as("o histórico guarda só as ideias novas").hasSize(1);
    }

    @Test
    void enviaAIaAsIdeiasJaSugeridasDaPessoaAsMaisRecentesPrimeiro() {
        gerar(ana, "Caneca de cerâmica");
        gerar(ana, "Livro de receitas");

        gerar(ana, "Moedor de café");

        assertThat(jaSugeridasRecebidas.getFirst()).isEmpty();
        assertThat(jaSugeridasRecebidas.getLast()).containsExactly("Livro de receitas", "Caneca de cerâmica");
    }

    @Test
    void naoRepeteOMesmoProdutoDaLojaAindaQueAIdeiaTenhaOutroNome() {
        ProdutoLoja prensa = new ProdutoLoja("Prensa Francesa Mimo 600ml", "https://www.amazon.com.br/dp/B07WXJ8GQP",
                null, new BigDecimal("89.90"));
        cenario.produtosNaLoja.put("Prensa francesa", List.of(prensa));
        cenario.produtosNaLoja.put("Cafeteira de êmbolo", List.of(prensa));
        gerar(ana, "Prensa francesa");

        ResultadoIdeias segunda = gerar(ana, "Cafeteira de êmbolo", "Livro de receitas");

        assertThat(nomes(segunda)).containsExactly("Livro de receitas");
    }

    @Test
    void ideiasRepetidasDentroDaMesmaGeracaoAparecemUmaVez() {
        ResultadoIdeias resultado = gerar(ana, "Caneca", "caneca", "Livro");

        assertThat(nomes(resultado)).containsExactly("Caneca", "Livro");
    }

    @Test
    void ideiasDeOutraPessoaPodemSerSugeridas() {
        Pessoa bruno = cenario.cadastrarPessoa("Bruno", List.of("café"), List.of());
        gerar(ana, "Caneca de cerâmica");

        assertThat(nomes(gerar(bruno, "Caneca de cerâmica"))).containsExactly("Caneca de cerâmica");
    }

    @Test
    void quandoTudoSeRepeteNadaEntraNoHistorico() {
        gerar(ana, "Caneca de cerâmica");

        ResultadoIdeias segunda = gerar(ana, "Caneca de cerâmica");

        assertThat(segunda.ideias()).isEmpty();
        assertThat(segunda.repetidas()).isEqualTo(1);
        assertThat(cenario.historico.listar(Cenario.USUARIO, ana.getId())).hasSize(1);
    }
}
