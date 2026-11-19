package com.gift4you.service;

import com.gift4you.exception.PessoaNaoEncontradaException;
import com.gift4you.model.Categoria;
import com.gift4you.model.DadosPessoa;
import com.gift4you.model.DadosPresente;
import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.Presente;
import com.gift4you.model.ResultadoIdeias;
import com.gift4you.model.SugestaoPresente;
import com.gift4you.model.Vinculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.gift4you.service.Cenario.USUARIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sugestões do catálogo: orçamento, gostos e interesses, ocasião e o que a pessoa não gosta.
 */
class SugestaoServiceTest {

    private Cenario cenario;

    @BeforeEach
    void preparar() {
        cenario = new Cenario();
    }

    private Pessoa pessoa(String orcamento, Ocasiao ocasiao, List<String> gostos, List<String> naoGosta) {
        return cenario.pessoas.cadastrar(USUARIO, new DadosPessoa("Ana", 30, Vinculo.AMIGO, gostos, naoGosta, ocasiao,
                new FaixaOrcamento(new BigDecimal(orcamento))));
    }

    private Presente presente(String nome, Categoria categoria, String preco, List<String> caracteristicas,
                              Ocasiao... ocasioes) {
        return cenario.presentes.cadastrar(Cenario.USUARIO, new DadosPresente(nome, categoria, "", new BigDecimal(preco),
                caracteristicas, List.of(ocasioes), Cenario.linkDe(nome)));
    }

    private List<String> nomesSugeridos(Pessoa pessoa) {
        return cenario.sugestoes.gerarSugestoes(USUARIO, pessoa.getId()).stream()
                .map(sugestao -> sugestao.presente().getNome()).toList();
    }

    @Nested
    class FiltroDeOrcamento {

        @Test
        void sugereSomentePresentesAteOOrcamentoMaximo() {
            Pessoa ana = pessoa("100", Ocasiao.NATAL, List.of("café"), List.of());
            presente("Caneca", Categoria.CASA, "40", List.of("café"), Ocasiao.NATAL);
            presente("Cafeteira", Categoria.CASA, "350", List.of("café"), Ocasiao.NATAL);

            assertThat(nomesSugeridos(ana)).containsExactly("Caneca");
        }

        @Test
        void presenteComPrecoIgualAoOrcamentoEntraNasSugestoes() {
            Pessoa ana = pessoa("100.00", Ocasiao.NATAL, List.of("café"), List.of());
            presente("Moedor", Categoria.CASA, "100", List.of("café"), Ocasiao.NATAL);
            presente("Moedor elétrico", Categoria.CASA, "100.01", List.of("café"), Ocasiao.NATAL);

            assertThat(nomesSugeridos(ana)).containsExactly("Moedor");
        }

        @Test
        void orcamentoAbaixoDeTodosOsPresentesNaoSugereNada() {
            Pessoa ana = pessoa("10", Ocasiao.NATAL, List.of("café"), List.of());
            presente("Caneca", Categoria.CASA, "40", List.of("café"), Ocasiao.NATAL);

            assertThat(cenario.sugestoes.gerarSugestoes(USUARIO, ana.getId())).isEmpty();
        }

        @Test
        void usaOOrcamentoAtualDepoisQueAPessoaEAlterada() {
            Pessoa ana = pessoa("30", Ocasiao.NATAL, List.of("café"), List.of());
            presente("Caneca", Categoria.CASA, "40", List.of("café"), Ocasiao.NATAL);
            assertThat(nomesSugeridos(ana)).isEmpty();

            cenario.pessoas.alterar(USUARIO, ana.getId(), new DadosPessoa("Ana", 30, Vinculo.AMIGO, List.of("café"),
                    List.of(), Ocasiao.NATAL, new FaixaOrcamento(new BigDecimal("50"))));

            assertThat(nomesSugeridos(ana)).containsExactly("Caneca");
        }

        @Test
        void ideiasDaIaForaDoOrcamentoSaoDescartadasEContadas() {
            Pessoa ana = pessoa("100", Ocasiao.NATAL, List.of("café"), List.of());
            cenario.ideiasDaIa.addAll(List.of(Cenario.ideia("Caneca", "40"), Cenario.ideia("Moedor", "100"),
                    Cenario.ideia("Cafeteira", "350"),
                    new IdeiaPresente("Sem preço", "Diversos", null, "Motivo")));

            ResultadoIdeias resultado = cenario.sugestoes.gerarIdeiasComIa(USUARIO, ana.getId());

            assertThat(resultado.ideias()).extracting(IdeiaPresente::nome).containsExactly("Caneca", "Moedor");
            assertThat(resultado.descartadas()).isEqualTo(2);
        }
    }

    @Nested
    class GeracaoDeSugestoes {

        @Test
        void pontuaGostosEOcasiaoEExplicaOsMotivos() {
            Pessoa ana = pessoa("500", Ocasiao.NATAL, List.of("café", "música"), List.of());
            presente("Caixa de som com café", Categoria.MUSICA, "200", List.of("música", "café"), Ocasiao.NATAL);

            SugestaoPresente sugestao = cenario.sugestoes.gerarSugestoes(USUARIO, ana.getId()).getFirst();

            assertThat(sugestao.pontuacao()).as("2 gostos x 2 pontos + 3 da ocasião").isEqualTo(7);
            assertThat(sugestao.motivos()).containsExactly("combina com os gostos: café, música", "adequado para Natal");
        }

        @Test
        void ordenaPelaPontuacaoEDepoisPeloMenorPreco() {
            Pessoa ana = pessoa("500", Ocasiao.NATAL, List.of("café", "música"), List.of());
            presente("Só ocasião", Categoria.OUTRA, "50", List.of(), Ocasiao.NATAL);
            presente("Gostos e ocasião", Categoria.OUTRA, "300", List.of("café", "música"), Ocasiao.NATAL);
            presente("Um gosto caro", Categoria.OUTRA, "250", List.of("café"), Ocasiao.ANIVERSARIO);
            presente("Um gosto barato", Categoria.OUTRA, "20", List.of("música"), Ocasiao.ANIVERSARIO);

            assertThat(nomesSugeridos(ana))
                    .containsExactly("Gostos e ocasião", "Só ocasião", "Um gosto barato", "Um gosto caro");
        }

        @Test
        void naoSugerePresentesSemRelacaoComGostosNemOcasiao() {
            Pessoa ana = pessoa("500", Ocasiao.NATAL, List.of("café"), List.of());
            presente("Bola", Categoria.ESPORTES, "80", List.of("futebol"), Ocasiao.ANIVERSARIO);

            assertThat(nomesSugeridos(ana)).isEmpty();
        }

        @Test
        void consideraOcasiaoMesmoSemGostosEmComum() {
            Pessoa ana = pessoa("500", Ocasiao.FORMATURA, List.of(), List.of());
            presente("Caneta", Categoria.OUTRA, "80", List.of(), Ocasiao.FORMATURA);
            presente("Árvore de Natal", Categoria.CASA, "80", List.of(), Ocasiao.NATAL);

            assertThat(nomesSugeridos(ana)).containsExactly("Caneta");
        }

        @Test
        void encontraGostosNoNomeNaDescricaoENaCategoriaIgnorandoAcentosEPlural() {
            Pessoa ana = pessoa("500", Ocasiao.NATAL, List.of("livros", "musica"), List.of());
            presente("Box de livro", Categoria.OUTRA, "80", List.of(), Ocasiao.ANIVERSARIO);
            presente("Vinil", Categoria.MUSICA, "90", List.of(), Ocasiao.ANIVERSARIO);

            assertThat(nomesSugeridos(ana)).containsExactlyInAnyOrder("Box de livro", "Vinil");
        }

        @Test
        void ignoraPresentesRelacionadosAoQueAPessoaNaoGosta() {
            Pessoa ana = pessoa("500", Ocasiao.NATAL, List.of("presente"), List.of("perfume", "beleza", "doces"));
            presente("Perfume floral", Categoria.OUTRA, "100", List.of(), Ocasiao.NATAL);
            presente("Kit de maquiagem", Categoria.BELEZA, "100", List.of(), Ocasiao.NATAL);
            presente("Cesta", Categoria.GASTRONOMIA, "100", List.of("doce"), Ocasiao.NATAL);
            presente("Livro", Categoria.LIVROS, "100", List.of(), Ocasiao.NATAL);

            assertThat(nomesSugeridos(ana)).as("rejeita pelo nome, pela categoria e pelas características")
                    .containsExactly("Livro");
        }

        @Test
        void ideiasDaIaRelacionadasAoQueAPessoaNaoGostaSaoDescartadas() {
            Pessoa ana = pessoa("500", Ocasiao.NATAL, List.of(), List.of("perfume"));
            cenario.ideiasDaIa.addAll(List.of(Cenario.ideia("Perfume importado", "150"), Cenario.ideia("Livro", "60")));

            ResultadoIdeias resultado = cenario.sugestoes.gerarIdeiasComIa(USUARIO, ana.getId());

            assertThat(resultado.ideias()).extracting(IdeiaPresente::nome).containsExactly("Livro");
            assertThat(resultado.descartadas()).isEqualTo(1);
        }

        @Test
        void limitaACincoSugestoes() {
            Pessoa ana = pessoa("500", Ocasiao.NATAL, List.of(), List.of());
            for (int i = 1; i <= 8; i++) {
                presente("Presente " + i, Categoria.OUTRA, String.valueOf(10 * i), List.of(), Ocasiao.NATAL);
            }

            assertThat(nomesSugeridos(ana))
                    .containsExactly("Presente 1", "Presente 2", "Presente 3", "Presente 4", "Presente 5");
        }

        @Test
        void catalogoVazioNaoSugereNada() {
            Pessoa ana = pessoa("500", Ocasiao.NATAL, List.of("café"), List.of());

            assertThat(cenario.sugestoes.gerarSugestoes(USUARIO, ana.getId())).isEmpty();
        }

        @Test
        void pessoaInexistenteAvisaQueNaoEncontrou() {
            assertThatThrownBy(() -> cenario.sugestoes.gerarSugestoes(USUARIO, 999))
                    .isInstanceOf(PessoaNaoEncontradaException.class);
        }
    }
}
