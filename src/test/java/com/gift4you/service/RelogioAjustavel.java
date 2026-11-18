package com.gift4you.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Relógio dos testes que só anda quando mandamos, para simular a passagem do tempo. */
class RelogioAjustavel extends Clock {

    private Instant agora = Instant.parse("2026-10-08T12:00:00Z");

    void avancar(Duration duracao) {
        agora = agora.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return agora;
    }
}
