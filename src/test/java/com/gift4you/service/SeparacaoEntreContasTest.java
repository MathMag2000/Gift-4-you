package com.gift4you.service;

import com.gift4you.exception.FavoritoNaoEncontradoException;
import com.gift4you.exception.PessoaNaoEncontradaException;
import com.gift4you.exception.PresenteNaoEncontradoException;
import com.gift4you.exception.RegistroHistoricoNaoEncontradoException;
import com.gift4you.model.Categoria;
import com.gift4you.model.DadosFavorito;
import com.gift4you.model.DadosPessoa;
import com.gift4you.model.DadosPresente;
import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.Favorito;
import com.gift4you.model.ItemSugerido;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.OrigemSugestao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.Presente;
import com.gift4you.model.Vinculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.gift4you.service.Cenario.OUTRO_USUARIO;
import static com.gift4you.service.Cenario.USUARIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * As pessoas pertencem à conta que as cadastrou: outra conta não as vê nem as altera, e o mesmo vale
 * para os favoritos e o histórico delas.
 */
class SeparacaoEntreContasTest {

    private Cenario cenario;
    private Pessoa ana;
    private Pessoa bruno;
    private Presente kitCafe;
    private Presente cafeteiraDoOutro;

    @BeforeEach
    void preparar() {
        cenario = new Cenario();
        ana = cenario.cadastrarPessoa(USUARIO, "Ana", List.of("café"), List.of());
        bruno = cenario.cadastrarPessoa(OUTRO_USUARIO, "Bruno", List.of("café"), List.of());
        kitCafe = cenario.cadastrarPresente("Kit de café", Categoria.GASTRONOMIA, "90", List.of("café"));
        cafeteiraDoOutro = cenario.presentes.cadastrar(OUTRO_USUARIO, new DadosPresente("Cafeteira",
                Categoria.CASA, "", new BigDecimal("150"), List.of("café"), List.of(Ocasiao.ANIVERSARIO),
                "https://loja.exemplo.com/cafeteira"));
    }

    /** Favorita, para a pessoa, o presente do catálogo da própria conta. */
    private Favorito favoritar(int usuarioId, Pessoa pessoa) {
        Presente presente = usuarioId == USUARIO ? kitCafe : cafeteiraDoOutro;
        return favoritar(usuarioId, pessoa, presente);
    }

    private Favorito favoritar(int usuarioId, Pessoa pessoa, Presente presente) {
        ItemSugerido item = new ItemSugerido(presente.getId(), null, null, null, List.of(), null, null);
        return cenario.favoritos.favoritar(usuarioId, new DadosFavorito(pessoa.getId(), OrigemSugestao.CATALOGO, item));
    }

    @Test
    void pessoaCadastradaFicaAssociadaAConta() {
        assertThat(ana.getUsuarioId()).isEqualTo(USUARIO);
        assertThat(cenario.pessoas.listar(USUARIO)).extracting(Pessoa::getNome).containsExactly("Ana");
        assertThat(cenario.pessoas.listar(OUTRO_USUARIO)).extracting(Pessoa::getNome).containsExactly("Bruno");
    }

    @Test
    void pessoaDeOutraContaNaoPodeSerConsultadaAlteradaOuRemovida() {
        DadosPessoa dados = new DadosPessoa("Invasor", 20, Vinculo.AMIGO, List.of(), List.of(), Ocasiao.NATAL,
                new FaixaOrcamento(new BigDecimal("50")));

        assertThatThrownBy(() -> cenario.pessoas.buscarPorId(OUTRO_USUARIO, ana.getId()))
                .isInstanceOf(PessoaNaoEncontradaException.class);
        assertThatThrownBy(() -> cenario.pessoas.alterar(OUTRO_USUARIO, ana.getId(), dados))
                .isInstanceOf(PessoaNaoEncontradaException.class);
        assertThatThrownBy(() -> cenario.pessoas.remover(OUTRO_USUARIO, ana.getId()))
                .isInstanceOf(PessoaNaoEncontradaException.class);

        assertThat(cenario.pessoas.buscarPorId(USUARIO, ana.getId()).getNome()).isEqualTo("Ana");
    }

    @Test
    void naoGeraSugestoesNemFavoritaParaPessoaDeOutraConta() {
        assertThatThrownBy(() -> cenario.sugestoes.gerarSugestoes(OUTRO_USUARIO, ana.getId()))
                .isInstanceOf(PessoaNaoEncontradaException.class);
        assertThatThrownBy(() -> cenario.sugestoes.gerarIdeiasComIa(OUTRO_USUARIO, ana.getId()))
                .isInstanceOf(PessoaNaoEncontradaException.class);
        assertThatThrownBy(() -> favoritar(OUTRO_USUARIO, ana))
                .isInstanceOf(PessoaNaoEncontradaException.class);
    }

    @Test
    void cadaContaVeERemoveSomenteOsSeusFavoritos() {
        Favorito daAna = favoritar(USUARIO, ana);
        favoritar(OUTRO_USUARIO, bruno);

        assertThat(cenario.favoritos.listar(USUARIO, null)).extracting(Favorito::getPessoaId)
                .containsExactly(ana.getId());
        assertThat(cenario.favoritos.listar(OUTRO_USUARIO, ana.getId())).isEmpty();
        assertThatThrownBy(() -> cenario.favoritos.remover(OUTRO_USUARIO, daAna.getId()))
                .isInstanceOf(FavoritoNaoEncontradoException.class);
        assertThat(cenario.favoritos.listar(USUARIO, ana.getId())).hasSize(1);
    }

    @Test
    void cadaContaVeSomenteOSeuHistorico() {
        cenario.sugestoes.gerarSugestoes(USUARIO, ana.getId());
        cenario.sugestoes.gerarSugestoes(OUTRO_USUARIO, bruno.getId());
        int registroDaAna = cenario.historico.listar(USUARIO, ana.getId()).getFirst().getId();

        assertThat(cenario.historico.listar(USUARIO, null)).singleElement()
                .satisfies(registro -> assertThat(registro.getPessoaId()).isEqualTo(ana.getId()));
        assertThat(cenario.historico.listar(OUTRO_USUARIO, ana.getId())).isEmpty();
        assertThatThrownBy(() -> cenario.historico.buscarPorId(OUTRO_USUARIO, registroDaAna))
                .isInstanceOf(RegistroHistoricoNaoEncontradoException.class);
    }

    @Test
    void cadaContaTemOProprioCatalogo() {
        assertThat(kitCafe.getUsuarioId()).isEqualTo(USUARIO);
        assertThat(cenario.presentes.listar(USUARIO)).extracting(Presente::getNome).containsExactly("Kit de café");
        assertThat(cenario.presentes.listar(OUTRO_USUARIO)).extracting(Presente::getNome).containsExactly("Cafeteira");
    }

    @Test
    void presenteDeOutraContaNaoPodeSerConsultadoAlteradoOuRemovido() {
        DadosPresente dados = new DadosPresente("Alterado", Categoria.OUTRA, "", BigDecimal.TEN, List.of(),
                List.of(Ocasiao.NATAL), "https://loja.exemplo.com/x");

        assertThatThrownBy(() -> cenario.presentes.buscarPorId(OUTRO_USUARIO, kitCafe.getId()))
                .isInstanceOf(PresenteNaoEncontradoException.class);
        assertThatThrownBy(() -> cenario.presentes.alterar(OUTRO_USUARIO, kitCafe.getId(), dados))
                .isInstanceOf(PresenteNaoEncontradoException.class);
        assertThatThrownBy(() -> cenario.presentes.remover(OUTRO_USUARIO, kitCafe.getId()))
                .isInstanceOf(PresenteNaoEncontradoException.class);

        assertThat(cenario.presentes.buscarPorId(USUARIO, kitCafe.getId()).getNome()).isEqualTo("Kit de café");
    }

    @Test
    void sugestoesUsamSomenteOCatalogoDaPropriaConta() {
        assertThat(cenario.sugestoes.gerarSugestoes(USUARIO, ana.getId()))
                .extracting(sugestao -> sugestao.presente().getNome()).containsExactly("Kit de café");
        assertThat(cenario.sugestoes.gerarSugestoes(OUTRO_USUARIO, bruno.getId()))
                .extracting(sugestao -> sugestao.presente().getNome()).containsExactly("Cafeteira");
    }

    @Test
    void naoFavoritaPresenteDoCatalogoDeOutraConta() {
        assertThatThrownBy(() -> favoritar(OUTRO_USUARIO, bruno, kitCafe))
                .isInstanceOf(PresenteNaoEncontradoException.class);
    }

    @Test
    void contaSemPessoasNaoTemFavoritosNemHistorico() {
        int contaNova = 3;

        assertThat(cenario.favoritos.listar(contaNova, null)).isEmpty();
        assertThat(cenario.historico.listar(contaNova, null)).isEmpty();
    }
}
