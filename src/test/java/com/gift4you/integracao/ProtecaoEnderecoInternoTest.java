package com.gift4you.integracao;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ProtecaoEnderecoInternoTest {

    private final ProtecaoEnderecoInterno protecao = new ProtecaoEnderecoInterno();

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080/api/pessoas",
            "http://127.0.0.1/",
            "http://0.0.0.0/",
            "http://[::1]/",
            "http://10.0.0.5/",
            "http://172.16.10.1/",
            "http://192.168.0.1/",
            "http://169.254.169.254/latest/meta-data/",
            "http://100.64.0.1/",
            "http://[fd00::1]/",
            "ftp://loja.com.br/arquivo",
            "file:///C:/Windows/win.ini",
            "http://endereco-que-nao-existe.invalid/"})
    void recusaEnderecosInternosOuInvalidos(String endereco) {
        assertThat(protecao.permitido(URI.create(endereco))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://8.8.8.8/", "https://1.1.1.1/produto"})
    void aceitaEnderecosPublicos(String endereco) {
        assertThat(protecao.permitido(URI.create(endereco))).isTrue();
    }
}
