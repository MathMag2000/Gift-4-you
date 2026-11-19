package com.gift4you.service;

import java.time.Clock;
import java.time.Duration;

/**
 * Limites que protegem as contas de abuso.
 *
 * @param falhasPorEmail     logins errados seguidos num mesmo e-mail (contra adivinhar a senha de uma conta)
 * @param falhasPorOrigem    logins errados vindos de um mesmo endereço IP (contra testar senhas em várias contas)
 * @param cadastrosPorOrigem contas criadas por um mesmo endereço IP (contra criar contas em massa)
 */
public record LimitesConta(LimiteDeTaxa falhasPorEmail, LimiteDeTaxa falhasPorOrigem,
                           LimiteDeTaxa cadastrosPorOrigem) {

    /** 5 erros por e-mail e 20 por IP a cada 15 minutos; 10 contas novas por IP a cada hora. */
    public static LimitesConta padrao(Clock relogio) {
        return new LimitesConta(new LimiteDeTaxa(5, Duration.ofMinutes(15), relogio),
                new LimiteDeTaxa(20, Duration.ofMinutes(15), relogio),
                new LimiteDeTaxa(10, Duration.ofHours(1), relogio));
    }
}
