package com.gift4you.service;

import com.gift4you.exception.PessoaNaoEncontradaException;
import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.DadosPessoa;
import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.Vinculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.gift4you.service.Cenario.USUARIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PessoaServiceTest {

    private Cenario cenario;

    @BeforeEach
    void preparar() {
        cenario = new Cenario();
    }

    /** Dados válidos; cada teste troca só o campo que quer conferir. */
    private static DadosPessoa dados(String nome, Integer idade, String orcamento) {
        return new DadosPessoa(nome, idade, Vinculo.MAE, List.of("café"), List.of("perfume"), Ocasiao.DIA_DAS_MAES,
                new FaixaOrcamento(orcamento == null ? null : new BigDecimal(orcamento)));
    }

    private static DadosPessoa dadosValidos() {
        return dados("Maria", 55, "300");
    }

    private Pessoa cadastrar(DadosPessoa dados) {
        return cenario.pessoas.cadastrar(USUARIO, dados);
    }

    @Nested
    class Cadastro {

        @Test
        void cadastraComTodosOsDadosEGeraId() {
            Pessoa pessoa = cadastrar(dadosValidos());

            assertThat(pessoa.getId()).isNotNull();
            assertThat(pessoa.getUsuarioId()).isEqualTo(USUARIO);
            assertThat(pessoa.getNome()).isEqualTo("Maria");
            assertThat(pessoa.getIdade()).isEqualTo(55);
            assertThat(pessoa.getVinculo()).isEqualTo(Vinculo.MAE);
            assertThat(pessoa.getGostos()).containsExactly("café");
            assertThat(pessoa.getNaoGosta()).containsExactly("perfume");
            assertThat(pessoa.getOcasiao()).isEqualTo(Ocasiao.DIA_DAS_MAES);
            assertThat(pessoa.getOrcamento().maximo()).isEqualByComparingTo("300");
            assertThat(cenario.pessoas.buscarPorId(USUARIO, pessoa.getId()).getNome()).isEqualTo("Maria");
        }

        @Test
        void geraIdsDiferentesEListaTodasAsPessoasDoUsuario() {
            Pessoa maria = cadastrar(dadosValidos());
            Pessoa jose = cadastrar(dados("José", 60, "200"));

            assertThat(jose.getId()).isNotEqualTo(maria.getId());
            assertThat(cenario.pessoas.listar(USUARIO)).extracting(Pessoa::getNome).containsExactly("Maria", "José");
        }

        @Test
        void normalizaGostosENaoGostaEmMinusculasSemVaziosNemRepetidos() {
            Pessoa pessoa = cadastrar(new DadosPessoa("Maria", 55, Vinculo.MAE,
                    Arrays.asList(" Café ", "café", "", null, "MÚSICA"), List.of(" Perfume "),
                    Ocasiao.NATAL, new FaixaOrcamento(new BigDecimal("100"))));

            assertThat(pessoa.getGostos()).containsExactly("café", "música");
            assertThat(pessoa.getNaoGosta()).containsExactly("perfume");
        }

        @Test
        void listasAusentesViramListasVazias() {
            Pessoa pessoa = cadastrar(new DadosPessoa("Maria", 55, Vinculo.MAE, null, null, Ocasiao.NATAL,
                    new FaixaOrcamento(new BigDecimal("100"))));

            assertThat(pessoa.getGostos()).isEmpty();
            assertThat(pessoa.getNaoGosta()).isEmpty();
        }

        @Test
        void naoGuardaUmaListaQueOChamadorPossaAlterarDepois() {
            List<String> gostos = new ArrayList<>(List.of("café"));
            Pessoa pessoa = cadastrar(new DadosPessoa("Maria", 55, Vinculo.MAE, gostos, List.of(), Ocasiao.NATAL,
                    new FaixaOrcamento(new BigDecimal("100"))));

            gostos.add("vinho");

            assertThat(pessoa.getGostos()).containsExactly("café");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 119, 120})
        void aceitaIdadesDentroDoLimite(int idade) {
            assertThat(cadastrar(dados("Maria", idade, "100")).getIdade()).isEqualTo(idade);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 121, 500})
        void recusaIdadesForaDoLimite(int idade) {
            assertThatThrownBy(() -> cadastrar(dados("Maria", idade, "100")))
                    .isInstanceOf(ValidacaoException.class).hasMessage("A idade deve estar entre 0 e 120 anos.");
        }

        @Test
        void recusaDadosObrigatoriosAusentes() {
            assertThatThrownBy(() -> cadastrar(dados("Maria", null, "100")))
                    .isInstanceOf(ValidacaoException.class).hasMessage("A idade é obrigatória.");
            assertThatThrownBy(() -> cadastrar(new DadosPessoa("Maria", 30, null, List.of(), List.of(), Ocasiao.NATAL,
                    new FaixaOrcamento(BigDecimal.TEN))))
                    .isInstanceOf(ValidacaoException.class).hasMessage("O vínculo é obrigatório.");
            assertThatThrownBy(() -> cadastrar(new DadosPessoa("Maria", 30, Vinculo.AMIGO, List.of(), List.of(), null,
                    new FaixaOrcamento(BigDecimal.TEN))))
                    .isInstanceOf(ValidacaoException.class).hasMessage("A ocasião é obrigatória.");
            assertThatThrownBy(() -> cadastrar(new DadosPessoa("Maria", 30, Vinculo.AMIGO, List.of(), List.of(),
                    Ocasiao.NATAL, null)))
                    .isInstanceOf(ValidacaoException.class).hasMessage("A faixa de orçamento é obrigatória.");
        }

        @Test
        void recusaMesmoItemEmGostosENaoGosta() {
            assertThatThrownBy(() -> cadastrar(new DadosPessoa("Maria", 30, Vinculo.AMIGO, List.of("Café", "vinho"),
                    List.of("café"), Ocasiao.NATAL, new FaixaOrcamento(BigDecimal.TEN))))
                    .isInstanceOf(ValidacaoException.class).hasMessageContaining("café");
        }

        @Test
        void dadosInvalidosNaoSaoGravados() {
            assertThatThrownBy(() -> cadastrar(dados("Maria", 200, "100"))).isInstanceOf(ValidacaoException.class);

            assertThat(cenario.pessoas.listar(USUARIO)).isEmpty();
        }
    }

    @Nested
    class ValidacaoDoNome {

        @ParameterizedTest
        @ValueSource(strings = {"Ana", "Li", "José da Silva", "Ana D'Ávila", "Jean-Luc", "João Jr.", "Zoë", "Ñandú",
                "Seu Zé", "Tia Ana Maria"})
        void aceitaNomesComAcentosEPontuacaoComum(String nome) {
            assertThat(cadastrar(dados(nome, 30, "100")).getNome()).isEqualTo(nome);
        }

        @Test
        void removeEspacosNasPontasERepetidos() {
            assertThat(cadastrar(dados("   Ana    Maria  ", 30, "100")).getNome()).isEqualTo("Ana Maria");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void recusaNomeVazio(String nome) {
            assertThatThrownBy(() -> cadastrar(dados(nome, 30, "100")))
                    .isInstanceOf(ValidacaoException.class).hasMessage("O nome é obrigatório.");
        }

        @Test
        void recusaNomeNulo() {
            assertThatThrownBy(() -> cadastrar(dados(null, 30, "100")))
                    .isInstanceOf(ValidacaoException.class).hasMessage("O nome é obrigatório.");
        }

        @Test
        void recusaNomeCurtoDemais() {
            assertThatThrownBy(() -> cadastrar(dados("A", 30, "100")))
                    .isInstanceOf(ValidacaoException.class).hasMessage("O nome deve ter pelo menos 2 caracteres.");
        }

        @Test
        void recusaNomeLongoDemais() {
            assertThat(cadastrar(dados("a".repeat(100), 30, "100")).getNome()).hasSize(100);
            assertThatThrownBy(() -> cadastrar(dados("a".repeat(101), 30, "100")))
                    .isInstanceOf(ValidacaoException.class).hasMessage("O nome deve ter no máximo 100 caracteres.");
        }

        @ParameterizedTest
        @ValueSource(strings = {"Ana2", "123", "Ana@", "<script>", "Ana_Maria", "Ana 😀", "--", "..", "An\u0000a"})
        void recusaNumerosSimbolosOuNomeSemLetras(String nome) {
            assertThatThrownBy(() -> cadastrar(dados(nome, 30, "100")))
                    .isInstanceOf(ValidacaoException.class)
                    .hasMessage("O nome deve conter apenas letras, espaços, apóstrofo, hífen ou ponto.");
        }
    }

    @Nested
    class Alteracao {

        @Test
        void alteraTodosOsDadosMantendoIdEDono() {
            Pessoa pessoa = cadastrar(dadosValidos());

            Pessoa alterada = cenario.pessoas.alterar(USUARIO, pessoa.getId(), new DadosPessoa(" Maria  Clara ", 56,
                    Vinculo.AVO, List.of("plantas"), List.of(), Ocasiao.NATAL, new FaixaOrcamento(new BigDecimal("80"))));

            assertThat(alterada.getId()).isEqualTo(pessoa.getId());
            assertThat(alterada.getUsuarioId()).isEqualTo(USUARIO);
            Pessoa consultada = cenario.pessoas.buscarPorId(USUARIO, pessoa.getId());
            assertThat(consultada.getNome()).isEqualTo("Maria Clara");
            assertThat(consultada.getIdade()).isEqualTo(56);
            assertThat(consultada.getVinculo()).isEqualTo(Vinculo.AVO);
            assertThat(consultada.getGostos()).containsExactly("plantas");
            assertThat(consultada.getNaoGosta()).isEmpty();
            assertThat(consultada.getOcasiao()).isEqualTo(Ocasiao.NATAL);
            assertThat(consultada.getOrcamento().maximo()).isEqualByComparingTo("80");
        }

        @Test
        void naoAlteraAsOutrasPessoas() {
            Pessoa maria = cadastrar(dadosValidos());
            Pessoa jose = cadastrar(dados("José", 60, "200"));

            cenario.pessoas.alterar(USUARIO, maria.getId(), dados("Maria Clara", 56, "100"));

            assertThat(cenario.pessoas.buscarPorId(USUARIO, jose.getId()).getNome()).isEqualTo("José");
            assertThat(cenario.pessoas.listar(USUARIO)).hasSize(2);
        }

        @Test
        void dadosInvalidosNaoAlteramAPessoa() {
            Pessoa pessoa = cadastrar(dadosValidos());

            assertThatThrownBy(() -> cenario.pessoas.alterar(USUARIO, pessoa.getId(), dados("Maria", 30, "0")))
                    .isInstanceOf(ValidacaoException.class).hasMessage("O orçamento máximo deve ser maior que zero.");
            assertThatThrownBy(() -> cenario.pessoas.alterar(USUARIO, pessoa.getId(), dados("M4ria", 30, "100")))
                    .isInstanceOf(ValidacaoException.class);

            Pessoa consultada = cenario.pessoas.buscarPorId(USUARIO, pessoa.getId());
            assertThat(consultada.getNome()).isEqualTo("Maria");
            assertThat(consultada.getIdade()).isEqualTo(55);
            assertThat(consultada.getOrcamento().maximo()).isEqualByComparingTo("300");
        }

        @Test
        void recusaAlterarPessoaInexistente() {
            assertThatThrownBy(() -> cenario.pessoas.alterar(USUARIO, 999, dadosValidos()))
                    .isInstanceOf(PessoaNaoEncontradaException.class).hasMessage("Pessoa com id 999 não encontrada.");
        }
    }

    @Nested
    class Remocao {

        @Test
        void removeSomenteAPessoaInformada() {
            Pessoa maria = cadastrar(dadosValidos());
            Pessoa jose = cadastrar(dados("José", 60, "200"));

            cenario.pessoas.remover(USUARIO, maria.getId());

            assertThat(cenario.pessoas.listar(USUARIO)).extracting(Pessoa::getId).containsExactly(jose.getId());
            assertThatThrownBy(() -> cenario.pessoas.buscarPorId(USUARIO, maria.getId()))
                    .isInstanceOf(PessoaNaoEncontradaException.class);
        }

        @Test
        void removerDuasVezesOuPessoaInexistenteAvisaQueNaoEncontrou() {
            Pessoa maria = cadastrar(dadosValidos());
            cenario.pessoas.remover(USUARIO, maria.getId());

            assertThatThrownBy(() -> cenario.pessoas.remover(USUARIO, maria.getId()))
                    .isInstanceOf(PessoaNaoEncontradaException.class);
            assertThatThrownBy(() -> cenario.pessoas.remover(USUARIO, 999))
                    .isInstanceOf(PessoaNaoEncontradaException.class);
        }

        @Test
        void avisaOsOuvintesSomenteQuandoRemove() {
            List<Integer> avisos = new ArrayList<>();
            cenario.pessoas.adicionarOuvinteRemocao(avisos::add);
            Pessoa maria = cadastrar(dadosValidos());

            cenario.pessoas.remover(USUARIO, maria.getId());
            assertThatThrownBy(() -> cenario.pessoas.remover(USUARIO, 999))
                    .isInstanceOf(PessoaNaoEncontradaException.class);

            assertThat(avisos).containsExactly(maria.getId());
        }
    }
}
