package com.gift4you.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimiteDeTaxaTest {

    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private final LimiteDeTaxa limite = new LimiteDeTaxa(3, Duration.ofMinutes(10), relogio);

    @Test
    void aceitaAteOMaximoERecusaODepois() {
        assertThat(limite.tentarRegistrar("a")).isTrue();
        assertThat(limite.tentarRegistrar("a")).isTrue();
        assertThat(limite.atingido("a")).isFalse();
        assertThat(limite.tentarRegistrar("a")).isTrue();

        assertThat(limite.atingido("a")).isTrue();
        assertThat(limite.tentarRegistrar("a")).as("o pedido recusado não conta").isFalse();
    }

    @Test
    void cadaChaveTemContagemPropria() {
        limite.registrar("a");
        limite.registrar("a");
        limite.registrar("a");

        assertThat(limite.atingido("a")).isTrue();
        assertThat(limite.atingido("b")).isFalse();
        assertThat(limite.tentarRegistrar("b")).isTrue();
    }

    @Test
    void eventosAntigosSaemDaJanela() {
        limite.registrar("a");
        relogio.avancar(Duration.ofMinutes(4));
        limite.registrar("a");
        limite.registrar("a");
        assertThat(limite.atingido("a")).isTrue();
        assertThat(limite.tempoParaLiberar("a")).isEqualTo(Duration.ofMinutes(6));

        relogio.avancar(Duration.ofMinutes(6));

        assertThat(limite.atingido("a")).as("o primeiro evento saiu da janela").isFalse();
        assertThat(limite.tempoParaLiberar("a")).isEqualTo(Duration.ofMinutes(4));
    }

    @Test
    void limparZeraAContagem() {
        limite.registrar("a");
        limite.registrar("a");
        limite.registrar("a");

        limite.limpar("a");

        assertThat(limite.atingido("a")).isFalse();
        assertThat(limite.tempoParaLiberar("a")).isZero();
    }

    @Test
    void mostraOTempoEmMinutosArredondandoParaCima() {
        assertThat(LimiteDeTaxa.emMinutos(Duration.ofSeconds(1))).isEqualTo("1 minuto");
        assertThat(LimiteDeTaxa.emMinutos(Duration.ofSeconds(61))).isEqualTo("2 minutos");
        assertThat(LimiteDeTaxa.emMinutos(Duration.ofMinutes(15))).isEqualTo("15 minutos");
        assertThat(LimiteDeTaxa.emMinutos(Duration.ZERO)).isEqualTo("1 minuto");
    }

    @Test
    void maximoPrecisaSerPositivo() {
        assertThatThrownBy(() -> new LimiteDeTaxa(0, Duration.ofMinutes(1), relogio))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
