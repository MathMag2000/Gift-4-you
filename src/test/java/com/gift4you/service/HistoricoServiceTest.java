package com.gift4you.service;

import com.gift4you.exception.IntegracaoIaException;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O histórico guarda somente as ideias geradas com IA e continua disponível depois que a pessoa é removida.
 */
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

    private void gerarIdeias(Pessoa pessoa, String... nomes) {
        cenario.ideiasDaIa.clear();
        for (String nome : nomes) {
            cenario.ideiasDaIa.add(Cenario.ideia(nome, "50"));
        }
        cenario.sugestoes.gerarIdeiasComIa(Cenario.USUARIO, pessoa.getId());
    }

    @Test
    void consultarSugestoesDoCatalogoNaoRegistraNoHistorico() {
        cenario.sugestoes.gerarSugestoes(Cenario.USUARIO, ana.getId());
        cenario.sugestoes.presentesDaPessoa(Cenario.USUARIO, ana.getId());

        assertThat(cenario.historico.listar(Cenario.USUARIO, null)).isEmpty();
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
        assertThat(registro.getPessoaId()).isEqualTo(ana.getId());
        assertThat(registro.getPessoaNome()).isEqualTo("Ana");
        assertThat(registro.isPessoaRemovida()).isFalse();
        assertThat(registro.getOcasiao()).isEqualTo(Ocasiao.ANIVERSARIO);
        assertThat(registro.getOrcamento().maximo()).isEqualByComparingTo("200");
        assertThat(registro.getRealizadoEm()).isEqualTo(LocalDateTime.of(2026, 10, 8, 12, 0));
        assertThat(registro.getItens()).extracting(ItemSugerido::nome).containsExactly("Prensa francesa");
        assertThat(registro.getItens().getFirst().presenteId()).isNull();
        assertThat(registro.getItens().getFirst().motivos()).containsExactly("Motivo de Prensa francesa");
        assertThat(registro.getItens().getFirst().linkCompra())
                .isEqualTo("https://www.amazon.com.br/s?k=Prensa+francesa");
    }

    @Test
    void geracaoSemIdeiasAprovadasNaoRegistra() {
        cenario.ideiasDaIa.add(Cenario.ideia("Viagem", "900"));

        cenario.sugestoes.gerarIdeiasComIa(Cenario.USUARIO, ana.getId());

        assertThat(cenario.historico.listar(Cenario.USUARIO, ana.getId())).isEmpty();
    }

    @Test
    void falhaDaIaNaoRegistra() {
        SugestaoService comFalha = new SugestaoService(cenario.pessoas, cenario.presentes, new ComparadorTermos(),
                (pessoa, quantidade) -> {
                    throw new IntegracaoIaException("A IA não respondeu.");
                }, cenario.historico, cenario.loja, new LimiteDeTaxa(10, Duration.ofHours(1), cenario.relogio));

        assertThatThrownBy(() -> comFalha.gerarIdeiasComIa(Cenario.USUARIO, ana.getId()))
                .isInstanceOf(IntegracaoIaException.class);
        assertThat(cenario.historico.listar(Cenario.USUARIO, null)).isEmpty();
    }

    @Test
    void guardaOPerfilDoMomentoMesmoQueAPessoaMudeDepois() {
        gerarIdeias(ana, "Prensa francesa");

        cenario.pessoas.alterar(Cenario.USUARIO, ana.getId(), new DadosPessoa("Ana Maria", 30, Vinculo.AMIGO,
                List.of("café"), List.of(), Ocasiao.NATAL, new FaixaOrcamento(new BigDecimal("20"))));

        RegistroHistorico registro = cenario.historico.listar(Cenario.USUARIO, ana.getId()).getFirst();
        assertThat(registro.getOcasiao()).isEqualTo(Ocasiao.ANIVERSARIO);
        assertThat(registro.getOrcamento().maximo()).isEqualByComparingTo("200");
        assertThat(registro.getPessoaNome()).isEqualTo("Ana");
    }

    @Test
    void filtraPorPessoaEListaOsMaisRecentesPrimeiro() {
        Pessoa bruno = cenario.cadastrarPessoa("Bruno", List.of(), List.of());
        gerarIdeias(ana, "Prensa francesa");
        gerarIdeias(bruno, "Livro");
        gerarIdeias(ana, "Caneca");

        assertThat(cenario.historico.listar(Cenario.USUARIO, ana.getId()))
                .extracting(registro -> registro.getItens().getFirst().nome())
                .containsExactly("Caneca", "Prensa francesa");
        assertThat(cenario.historico.listar(Cenario.USUARIO, null)).hasSize(3);
    }

    @Test
    void buscaRegistroPorId() {
        gerarIdeias(ana, "Prensa francesa");
        int id = cenario.historico.listar(Cenario.USUARIO, ana.getId()).getFirst().getId();

        assertThat(cenario.historico.buscarPorId(Cenario.USUARIO, id).getPessoaId()).isEqualTo(ana.getId());
        assertThatThrownBy(() -> cenario.historico.buscarPorId(Cenario.USUARIO, 999))
                .isInstanceOf(RegistroHistoricoNaoEncontradoException.class);
    }

    @Test
    void removerPessoaMantemOHistoricoIndicandoQueElaFoiExcluida() {
        Pessoa bruno = cenario.cadastrarPessoa("Bruno", List.of(), List.of());
        gerarIdeias(ana, "Prensa francesa");
        gerarIdeias(bruno, "Livro");

        cenario.pessoas.remover(Cenario.USUARIO, ana.getId());

        List<RegistroHistorico> registros = cenario.historico.listar(Cenario.USUARIO, null);
        assertThat(registros).hasSize(2);
        RegistroHistorico daAna = registros.stream()
                .filter(registro -> registro.getPessoaNome().equals("Ana")).findFirst().orElseThrow();
        assertThat(daAna.isPessoaRemovida()).isTrue();
        assertThat(daAna.getPessoaId()).isNull();
        assertThat(daAna.getItens()).extracting(ItemSugerido::nome).containsExactly("Prensa francesa");
        assertThat(cenario.historico.buscarPorId(Cenario.USUARIO, daAna.getId()).getPessoaNome()).isEqualTo("Ana");
    }
}
