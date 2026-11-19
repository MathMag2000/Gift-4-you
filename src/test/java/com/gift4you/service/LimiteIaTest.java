package com.gift4you.service;

import com.gift4you.exception.IntegracaoIaException;
import com.gift4you.exception.LimiteExcedidoException;
import com.gift4you.model.Pessoa;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static com.gift4you.service.Cenario.OUTRO_USUARIO;
import static com.gift4you.service.Cenario.USUARIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cada conta pode pedir um número limitado de ideias à IA por hora. */
class LimiteIaTest {

    private final Cenario cenario = new Cenario();
    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private int chamadasAIa = 0;
    private boolean iaFalha = false;

    private final SugestaoService sugestoes = new SugestaoService(cenario.pessoas, cenario.presentes,
            new ComparadorTermos(), (pessoa, quantidade) -> {
                chamadasAIa++;
                if (iaFalha) {
                    throw new IntegracaoIaException("IA fora do ar.");
                }
                return List.of(Cenario.ideia("Caneca", "40"));
            }, cenario.historico, cenario.loja, new LimiteDeTaxa(2, Duration.ofHours(1), relogio));

    @Test
    void recusaPedidosAlemDoLimiteSemChamarAIa() {
        Pessoa ana = cenario.cadastrarPessoa("Ana", List.of(), List.of());
        sugestoes.gerarIdeiasComIa(USUARIO, ana.getId());
        sugestoes.gerarIdeiasComIa(USUARIO, ana.getId());

        assertThatThrownBy(() -> sugestoes.gerarIdeiasComIa(USUARIO, ana.getId()))
                .isInstanceOf(LimiteExcedidoException.class)
                .hasMessage("Você atingiu o limite de 2 pedidos de ideias com IA por hora. Tente novamente em 60 minutos.");
        assertThat(chamadasAIa).isEqualTo(2);
        assertThat(cenario.historico.listar(USUARIO, ana.getId())).as("pedido recusado não vai ao histórico").hasSize(2);
    }

    @Test
    void liberaDepoisDeUmaHora() {
        Pessoa ana = cenario.cadastrarPessoa("Ana", List.of(), List.of());
        sugestoes.gerarIdeiasComIa(USUARIO, ana.getId());
        sugestoes.gerarIdeiasComIa(USUARIO, ana.getId());

        relogio.avancar(Duration.ofHours(1));

        assertThat(sugestoes.gerarIdeiasComIa(USUARIO, ana.getId()).ideias()).hasSize(1);
    }

    @Test
    void oLimiteEDeCadaConta() {
        Pessoa ana = cenario.cadastrarPessoa("Ana", List.of(), List.of());
        Pessoa bruno = cenario.cadastrarPessoa(OUTRO_USUARIO, "Bruno", List.of(), List.of());
        sugestoes.gerarIdeiasComIa(USUARIO, ana.getId());
        sugestoes.gerarIdeiasComIa(USUARIO, ana.getId());

        assertThat(sugestoes.gerarIdeiasComIa(OUTRO_USUARIO, bruno.getId()).ideias()).hasSize(1);
    }

    @Test
    void pedidosQueFalhamTambemContam() {
        Pessoa ana = cenario.cadastrarPessoa("Ana", List.of(), List.of());
        iaFalha = true;
        assertThatThrownBy(() -> sugestoes.gerarIdeiasComIa(USUARIO, ana.getId())).isInstanceOf(IntegracaoIaException.class);
        assertThatThrownBy(() -> sugestoes.gerarIdeiasComIa(USUARIO, ana.getId())).isInstanceOf(IntegracaoIaException.class);

        assertThatThrownBy(() -> sugestoes.gerarIdeiasComIa(USUARIO, ana.getId()))
                .isInstanceOf(LimiteExcedidoException.class);
    }

    @Test
    void pessoaDeOutraContaNaoGastaOLimite() {
        Pessoa ana = cenario.cadastrarPessoa("Ana", List.of(), List.of());
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> sugestoes.gerarIdeiasComIa(OUTRO_USUARIO, ana.getId()))
                    .isInstanceOf(com.gift4you.exception.PessoaNaoEncontradaException.class);
        }

        assertThat(chamadasAIa).isZero();
        assertThat(sugestoes.gerarIdeiasComIa(OUTRO_USUARIO, cenario.cadastrarPessoa(OUTRO_USUARIO, "Bruno",
                List.of(), List.of()).getId()).ideias()).hasSize(1);
    }
}
