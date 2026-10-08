package com.gift4you.service;

import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.Categoria;
import com.gift4you.model.DadosPresente;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.Presente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PresenteServiceTest {

    private final Cenario cenario = new Cenario();

    private DadosPresente dados(String link) {
        return new DadosPresente("Fone", Categoria.ELETRONICOS, "", new BigDecimal("150"), List.of(),
                List.of(Ocasiao.NATAL), link);
    }

    @Test
    void cadastrarBuscaAImagemNaPaginaDoLink() {
        Presente presente = cenario.presentes.cadastrar(dados("  https://loja.exemplo.com/fone  "));

        assertThat(presente.getLinkCompra()).isEqualTo("https://loja.exemplo.com/fone");
        assertThat(presente.getImagemUrl()).isEqualTo("https://loja.exemplo.com/fone/foto.jpg");
    }

    @Test
    void cadastraMesmoQuandoALojaNaoPermiteLerAImagem() {
        Presente presente = cenario.presentes.cadastrar(dados("https://loja.exemplo.com/sem-imagem"));

        assertThat(presente.getId()).isNotNull();
        assertThat(presente.getImagemUrl()).isNull();
    }

    @Test
    void alterarSemMudarOLinkNaoBuscaAImagemDeNovo() {
        Presente presente = cenario.presentes.cadastrar(dados("https://loja.exemplo.com/fone"));

        cenario.presentes.alterar(presente.getId(), dados("https://loja.exemplo.com/fone"));

        assertThat(cenario.linksConsultados).hasSize(1);
    }

    @Test
    void alterarOLinkBuscaANovaImagem() {
        Presente presente = cenario.presentes.cadastrar(dados("https://loja.exemplo.com/fone"));

        Presente alterado = cenario.presentes.alterar(presente.getId(), dados("https://outra.exemplo.com/fone"));

        assertThat(alterado.getImagemUrl()).isEqualTo("https://outra.exemplo.com/fone/foto.jpg");
    }

    @Test
    void alterarTentaDeNovoQuandoAImagemNaoTinhaSidoEncontrada() {
        Presente presente = cenario.presentes.cadastrar(dados("https://loja.exemplo.com/sem-imagem"));

        cenario.presentes.alterar(presente.getId(), dados("https://loja.exemplo.com/sem-imagem"));

        assertThat(cenario.linksConsultados).hasSize(2);
    }

    @Test
    void linkDeCompraEObrigatorio() {
        assertThatThrownBy(() -> cenario.presentes.cadastrar(dados("  ")))
                .isInstanceOf(ValidacaoException.class).hasMessage("O link para compra é obrigatório.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "www.loja.com.br/fone", "ftp://loja.com.br/fone",
            "https://", "https://loja com espaço.com", "data:text/html,oi"})
    void recusaLinksInvalidos(String link) {
        assertThatThrownBy(() -> cenario.presentes.cadastrar(dados(link)))
                .isInstanceOf(ValidacaoException.class).hasMessageContaining("link para compra válido");
        assertThat(cenario.linksConsultados).isEmpty();
    }

    @Test
    void recusaLinkLongoDemais() {
        String link = "https://loja.exemplo.com/" + "a".repeat(2000);

        assertThatThrownBy(() -> cenario.presentes.cadastrar(dados(link)))
                .isInstanceOf(ValidacaoException.class).hasMessage("O link para compra é longo demais.");
    }
}
