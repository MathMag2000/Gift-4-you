package com.gift4you.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CodificadorSenhaPbkdf2Test {

    private final CodificadorSenhaPbkdf2 codificador = new CodificadorSenhaPbkdf2(1000);

    @Test
    void confereASenhaCorretaERecusaAsOutras() {
        String hash = codificador.codificar("senha1234");

        assertThat(codificador.confere("senha1234", hash)).isTrue();
        assertThat(codificador.confere("senha12345", hash)).isFalse();
        assertThat(codificador.confere("", hash)).isFalse();
        assertThat(codificador.confere(null, hash)).isFalse();
    }

    @Test
    void mesmaSenhaGeraHashesDiferentesPorCausaDoSal() {
        assertThat(codificador.codificar("senha1234")).isNotEqualTo(codificador.codificar("senha1234"));
    }

    @Test
    void guardaAsIteracoesNoHashParaConferirMesmoSeOPadraoMudar() {
        String hashAntigo = codificador.codificar("senha1234");

        assertThat(hashAntigo).startsWith("pbkdf2_sha256$1000$");
        assertThat(new CodificadorSenhaPbkdf2(2000).confere("senha1234", hashAntigo)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "texto qualquer", "pbkdf2_sha256$abc$sal$hash", "md5$1000$c2Fs$aGFzaA==",
            "pbkdf2_sha256$1000$!!!$???"})
    void hashMalformadoNuncaConfere(String hash) {
        assertThat(codificador.confere("senha1234", hash)).isFalse();
    }
}
