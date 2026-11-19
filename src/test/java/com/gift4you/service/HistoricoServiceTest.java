package com.gift4you.service;

import com.gift4you.exception.RegistroHistoricoNaoEncontradoException;
import com.gift4you.model.Categoria;
import com.gift4you.model.DadosPessoa;
import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.ItemSugerido;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.OrigemSugestao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.RegistroHistorico;
import com.gift4you.model.Vinculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HistoricoServiceTest {

    private Cenario cenario;
    private Pessoa ana;

    @BeforeEach
    void preparar() {
        cenario = new Cenario();
        ana = cenario.cadastrarPessoa("Ana", List.of("café"), List.of("perfume"));
        cenario.cadastrarPresente("Kit de café", Categoria.GASTRONOMIA, "90", List.of("café"));
        cenario.cadastrarPresente("Perfume", Categoria.BELEZA, "100", List.of());
    }

    @Test
    void gerarSugestoesDoCatalogoRegistraNoHistorico() {
        cenario.sugestoes.gerarSugestoes(Cenario.USUARIO, ana.getId());

        RegistroHistorico registro = cenario.historico.listar(Cenario.USUARIO, ana.getId()).getFirst();
        assertThat(registro.getOrigem()).isEqualTo(OrigemSugestao.CATALOGO);
        assertThat(registro.getOcasiao()).isEqualTo(Ocasiao.ANIVERSARIO);
        assertThat(registro.getOrcamento().maximo()).isEqualByComparingTo("200");
        assertThat(registro.getRealizadoEm()).isEqualTo(LocalDateTime.of(2026, 10, 8, 12, 0));
        assertThat(registro.getItens()).singleElement().satisfies(item -> {
            assertThat(item.nome()).isEqualTo("Kit de café");
            assertThat(item.categoria()).isEqualTo("Gastronomia");
            assertThat(item.presenteId()).isNotNull();
            assertThat(item.motivos()).contains("combina com os gostos: café");
        });
    }

    @Test
    void gerarIdeiasComIaRegistraSomenteAsAprovadas() {
        cenario.ideiasDaIa.addAll(List.of(
                Cenario.ideia("Prensa francesa", "120"),
                Cenario.ideia("Perfume importado", "150"),
                Cenario.ideia("Viagem", "900")));

        cenario.sugestoes.gerarIdeiasComIa(Cenario.USUARIO, ana.getId());

        RegistroHistorico registro = cenario.historico.listar(Cenario.USUARIO, ana.getId()).getFirst();
        assertThat(registro.getOrigem()).isEqualTo(OrigemSugestao.IA);
        assertThat(registro.getItens()).extracting(ItemSugerido::nome).containsExactly("Prensa francesa");
        assertThat(registro.getItens().getFirst().presenteId()).isNull();
        assertThat(registro.getItens().getFirst().motivos()).containsExactly("Motivo de Prensa francesa");
        assertThat(registro.getItens().getFirst().linkCompra())
                .isEqualTo("https://www.amazon.com.br/s?k=Prensa+francesa");
    }

    @Test
    void registraMesmoQuandoNenhumaSugestaoEEncontrada() {
        Pessoa semAfinidade = cenario.pessoas.cadastrar(Cenario.USUARIO, new DadosPessoa("Caio", 20, Vinculo.IRMAO, List.of(),
                List.of(), Ocasiao.FORMATURA,
                new FaixaOrcamento(new BigDecimal("2000"))));

        cenario.sugestoes.gerarSugestoes(Cenario.USUARIO, semAfinidade.getId());

        assertThat(cenario.historico.listar(Cenario.USUARIO, semAfinidade.getId())).singleElement()
                .satisfies(registro -> assertThat(registro.getItens()).isEmpty());
    }

    @Test
    void guardaOPerfilDoMomentoMesmoQueAPessoaMudeDepois() {
        cenario.sugestoes.gerarSugestoes(Cenario.USUARIO, ana.getId());

        cenario.pessoas.alterar(Cenario.USUARIO, ana.getId(), new DadosPessoa("Ana", 30, Vinculo.AMIGO, List.of("café"),
                List.of(), Ocasiao.NATAL, new FaixaOrcamento(new BigDecimal("20"))));

        RegistroHistorico registro = cenario.historico.listar(Cenario.USUARIO, ana.getId()).getFirst();
        assertThat(registro.getOcasiao()).isEqualTo(Ocasiao.ANIVERSARIO);
        assertThat(registro.getOrcamento().maximo()).isEqualByComparingTo("200");
    }

    @Test
    void filtraPorPessoaEListaOsMaisRecentesPrimeiro() {
        Pessoa bruno = cenario.cadastrarPessoa("Bruno", List.of(), List.of());
        cenario.sugestoes.gerarSugestoes(Cenario.USUARIO, ana.getId());
        cenario.sugestoes.gerarSugestoes(Cenario.USUARIO, bruno.getId());
        cenario.sugestoes.gerarIdeiasComIa(Cenario.USUARIO, ana.getId());

        assertThat(cenario.historico.listar(Cenario.USUARIO, ana.getId())).extracting(RegistroHistorico::getOrigem)
                .containsExactly(OrigemSugestao.IA, OrigemSugestao.CATALOGO);
        assertThat(cenario.historico.listar(Cenario.USUARIO, null)).hasSize(3);
    }

    @Test
    void buscaRegistroPorId() {
        cenario.sugestoes.gerarSugestoes(Cenario.USUARIO, ana.getId());
        int id = cenario.historico.listar(Cenario.USUARIO, ana.getId()).getFirst().getId();

        assertThat(cenario.historico.buscarPorId(Cenario.USUARIO, id).getPessoaId()).isEqualTo(ana.getId());
        assertThatThrownBy(() -> cenario.historico.buscarPorId(Cenario.USUARIO, 999))
                .isInstanceOf(RegistroHistoricoNaoEncontradoException.class);
    }

    @Test
    void removerPessoaApagaOHistoricoDela() {
        Pessoa bruno = cenario.cadastrarPessoa("Bruno", List.of(), List.of());
        cenario.sugestoes.gerarSugestoes(Cenario.USUARIO, ana.getId());
        cenario.sugestoes.gerarSugestoes(Cenario.USUARIO, bruno.getId());

        cenario.pessoas.remover(Cenario.USUARIO, ana.getId());

        assertThat(cenario.historico.listar(Cenario.USUARIO, null)).extracting(RegistroHistorico::getPessoaId)
                .containsExactly(bruno.getId());
    }
}
