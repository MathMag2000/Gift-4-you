package com.gift4you.service;

import com.gift4you.exception.FavoritoNaoEncontradoException;
import com.gift4you.exception.PessoaNaoEncontradaException;
import com.gift4you.exception.PresenteNaoEncontradoException;
import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.Categoria;
import com.gift4you.model.DadosFavorito;
import com.gift4you.model.Favorito;
import com.gift4you.model.ItemSugerido;
import com.gift4you.model.OrigemSugestao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.Presente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FavoritoServiceTest {

    private Cenario cenario;
    private Pessoa ana;
    private Presente kitCafe;

    @BeforeEach
    void preparar() {
        cenario = new Cenario();
        ana = cenario.cadastrarPessoa("Ana", List.of("café"), List.of());
        kitCafe = cenario.cadastrarPresente("Kit de café", Categoria.GASTRONOMIA, "90", List.of("café"));
    }

    private Favorito favoritarDoCatalogo(int pessoaId, int presenteId) {
        ItemSugerido item = new ItemSugerido(presenteId, null, null, null, List.of("combina com os gostos: café"));
        return cenario.favoritos.favoritar(new DadosFavorito(pessoaId, OrigemSugestao.CATALOGO, item));
    }

    private Favorito favoritarIdeia(int pessoaId, String nome, String preco) {
        ItemSugerido item = new ItemSugerido(null, nome, " Utensílios ", preco == null ? null : new BigDecimal(preco),
                List.of("Bom para quem gosta de café", " "));
        return cenario.favoritos.favoritar(new DadosFavorito(pessoaId, OrigemSugestao.IA, item));
    }

    @Test
    void favoritarDoCatalogoUsaOsDadosDoCatalogoENaoOsEnviados() {
        ItemSugerido enviado = new ItemSugerido(kitCafe.getId(), "Nome falso", "Outra", new BigDecimal("1"), List.of());

        Favorito favorito = cenario.favoritos.favoritar(new DadosFavorito(ana.getId(), OrigemSugestao.CATALOGO, enviado));

        assertThat(favorito.getId()).isNotNull();
        assertThat(favorito.getItem().nome()).isEqualTo("Kit de café");
        assertThat(favorito.getItem().categoria()).isEqualTo("Gastronomia");
        assertThat(favorito.getItem().preco()).isEqualByComparingTo("90");
        assertThat(favorito.getFavoritadoEm()).isEqualTo(LocalDateTime.of(2026, 10, 8, 12, 0));
    }

    @Test
    void favoritarDuasVezesDevolveOMesmoFavorito() {
        Favorito primeiro = favoritarDoCatalogo(ana.getId(), kitCafe.getId());
        Favorito segundo = favoritarDoCatalogo(ana.getId(), kitCafe.getId());

        assertThat(segundo.getId()).isEqualTo(primeiro.getId());
        assertThat(cenario.favoritos.listar(ana.getId())).hasSize(1);
    }

    @Test
    void ideiaDaIaRepetidaEReconhecidaPeloNomeIgnorandoMaiusculas() {
        Favorito primeiro = favoritarIdeia(ana.getId(), "Prensa Francesa", "120");
        Favorito segundo = favoritarIdeia(ana.getId(), "  prensa francesa ", "120");

        assertThat(segundo.getId()).isEqualTo(primeiro.getId());
    }

    @Test
    void ideiaDaIaENormalizada() {
        Favorito favorito = favoritarIdeia(ana.getId(), "  Prensa Francesa  ", "120");

        assertThat(favorito.getItem().nome()).isEqualTo("Prensa Francesa");
        assertThat(favorito.getItem().categoria()).isEqualTo("Utensílios");
        assertThat(favorito.getItem().presenteId()).isNull();
        assertThat(favorito.getItem().motivos()).containsExactly("Bom para quem gosta de café");
    }

    @Test
    void mesmoItemPodeSerFavoritoDePessoasDiferentes() {
        Pessoa bruno = cenario.cadastrarPessoa("Bruno", List.of(), List.of());

        favoritarDoCatalogo(ana.getId(), kitCafe.getId());
        favoritarDoCatalogo(bruno.getId(), kitCafe.getId());

        assertThat(cenario.favoritos.listar(null)).hasSize(2);
        assertThat(cenario.favoritos.listar(bruno.getId())).extracting(Favorito::getPessoaId).containsOnly(bruno.getId());
    }

    @Test
    void listaOsMaisRecentesPrimeiro() {
        Favorito primeiro = favoritarDoCatalogo(ana.getId(), kitCafe.getId());
        Favorito segundo = favoritarIdeia(ana.getId(), "Prensa Francesa", "120");

        assertThat(cenario.favoritos.listar(ana.getId())).extracting(Favorito::getId)
                .containsExactly(segundo.getId(), primeiro.getId());
    }

    @Test
    void recusaPedidosInvalidos() {
        assertThatThrownBy(() -> favoritarIdeia(ana.getId(), " ", "120"))
                .isInstanceOf(ValidacaoException.class).hasMessageContaining("nome");
        assertThatThrownBy(() -> favoritarIdeia(ana.getId(), "Caneca", "0"))
                .isInstanceOf(ValidacaoException.class).hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> favoritarIdeia(ana.getId(), "Caneca", null))
                .isInstanceOf(ValidacaoException.class).hasMessageContaining("preço");
        assertThatThrownBy(() -> cenario.favoritos.favoritar(new DadosFavorito(ana.getId(), null, null)))
                .isInstanceOf(ValidacaoException.class).hasMessageContaining("origem");
        assertThatThrownBy(() -> favoritarDoCatalogo(ana.getId(), 999))
                .isInstanceOf(PresenteNaoEncontradoException.class);
        assertThatThrownBy(() -> favoritarDoCatalogo(999, kitCafe.getId()))
                .isInstanceOf(PessoaNaoEncontradaException.class);
    }

    @Test
    void removeFavorito() {
        Favorito favorito = favoritarDoCatalogo(ana.getId(), kitCafe.getId());

        cenario.favoritos.remover(favorito.getId());

        assertThat(cenario.favoritos.listar(ana.getId())).isEmpty();
        assertThatThrownBy(() -> cenario.favoritos.remover(favorito.getId()))
                .isInstanceOf(FavoritoNaoEncontradoException.class);
    }

    @Test
    void removerPessoaApagaSomenteOsFavoritosDela() {
        Pessoa bruno = cenario.cadastrarPessoa("Bruno", List.of(), List.of());
        favoritarDoCatalogo(ana.getId(), kitCafe.getId());
        favoritarDoCatalogo(bruno.getId(), kitCafe.getId());

        cenario.pessoas.remover(ana.getId());

        assertThat(cenario.favoritos.listar(null)).extracting(Favorito::getPessoaId).containsExactly(bruno.getId());
    }

    @Test
    void favoritoContinuaExistindoSeOPresenteMudarNoCatalogo() {
        favoritarDoCatalogo(ana.getId(), kitCafe.getId());

        cenario.presentes.remover(kitCafe.getId());

        assertThat(cenario.favoritos.listar(ana.getId())).singleElement()
                .satisfies(favorito -> assertThat(favorito.getItem().nome()).isEqualTo("Kit de café"));
    }
}
