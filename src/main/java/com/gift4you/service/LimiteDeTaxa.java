package com.gift4you.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Conta eventos por chave (ex.: "ia:12", "login:ana@x.com") numa janela de tempo deslizante e diz quando o
 * máximo foi atingido. Os dados ficam na memória do servidor: reiniciar zera as contagens.
 */
public class LimiteDeTaxa {

    private final int maximo;
    private final Duration janela;
    private final Clock relogio;
    private final Map<String, Deque<Instant>> eventos = new ConcurrentHashMap<>();

    public LimiteDeTaxa(int maximo, Duration janela, Clock relogio) {
        if (maximo < 1) {
            throw new IllegalArgumentException("O máximo deve ser pelo menos 1.");
        }
        this.maximo = maximo;
        this.janela = janela;
        this.relogio = relogio;
    }

    /** Indica se a chave já atingiu o máximo de eventos dentro da janela. */
    public boolean atingido(String chave) {
        Deque<Instant> registros = eventos.get(chave);
        if (registros == null) {
            return false;
        }
        synchronized (registros) {
            descartarAntigos(registros);
            return registros.size() >= maximo;
        }
    }

    public void registrar(String chave) {
        Deque<Instant> registros = eventos.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (registros) {
            descartarAntigos(registros);
            registros.addLast(relogio.instant());
        }
    }

    /**
     * Registra o evento se o máximo ainda não foi atingido, numa só operação.
     *
     * @return false se o máximo já tinha sido atingido (o evento não é registrado)
     */
    public boolean tentarRegistrar(String chave) {
        Deque<Instant> registros = eventos.computeIfAbsent(chave, k -> new ArrayDeque<>());
        synchronized (registros) {
            descartarAntigos(registros);
            if (registros.size() >= maximo) {
                return false;
            }
            registros.addLast(relogio.instant());
            return true;
        }
    }

    public void limpar(String chave) {
        eventos.remove(chave);
    }

    /** Tempo até o evento mais antigo sair da janela, liberando um novo pedido. */
    public Duration tempoParaLiberar(String chave) {
        Deque<Instant> registros = eventos.get(chave);
        if (registros == null) {
            return Duration.ZERO;
        }
        synchronized (registros) {
            descartarAntigos(registros);
            if (registros.isEmpty()) {
                return Duration.ZERO;
            }
            Duration restante = Duration.between(relogio.instant(), registros.peekFirst().plus(janela));
            return restante.isNegative() ? Duration.ZERO : restante;
        }
    }

    public int getMaximo() {
        return maximo;
    }

    public Duration getJanela() {
        return janela;
    }

    private void descartarAntigos(Deque<Instant> registros) {
        Instant limite = relogio.instant().minus(janela);
        while (!registros.isEmpty() && !registros.peekFirst().isAfter(limite)) {
            registros.pollFirst();
        }
    }

    /** "1 minuto", "15 minutos": arredonda para cima, nunca menos de 1 minuto. */
    static String emMinutos(Duration duracao) {
        long minutos = Math.max(1, (duracao.toSeconds() + 59) / 60);
        return minutos == 1 ? "1 minuto" : minutos + " minutos";
    }
}
