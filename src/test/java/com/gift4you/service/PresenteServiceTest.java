package com.gift4you.service;

import com.gift4you.exception.PresenteNaoEncontradoException;
import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.Categoria;
import com.gift4you.model.DadosPresente;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.Presente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
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
    void cadastraComTodosOsDadosNormalizados() {
        Presente presente = cenario.presentes.cadastrar(new DadosPresente("  Fone   Bluetooth 5.0 ", Categoria.ELETRONICOS,
                "  Sem fio  ", new BigDecimal("149.90"), Arrays.asList(" Música ", "música", "", null, "SEM FIO"),
                Arrays.asList(Ocasiao.NATAL, Ocasiao.ANIVERSARIO, Ocasiao.NATAL, null), "https://loja.exemplo.com/fone"));

        assertThat(presente.getId()).isNotNull();
        assertThat(presente.getNome()).isEqualTo("Fone Bluetooth 5.0");
        assertThat(presente.getCategoria()).isEqualTo(Categoria.ELETRONICOS);
        assertThat(presente.getDescricao()).isEqualTo("Sem fio");
        assertThat(presente.getPreco()).isEqualByComparingTo("149.90");
        assertThat(presente.getCaracteristicas()).containsExactly("música", "sem fio");
        assertThat(presente.getOcasioes()).containsExactly(Ocasiao.ANIVERSARIO, Ocasiao.NATAL);
        assertThat(cenario.presentes.buscarPorId(presente.getId()).getNome()).isEqualTo("Fone Bluetooth 5.0");
    }

    @Test
    void descricaoECaracteristicasSaoOpcionais() {
        Presente presente = cenario.presentes.cadastrar(new DadosPresente("Fone", Categoria.ELETRONICOS, null,
                new BigDecimal("150"), null, List.of(Ocasiao.NATAL), "https://loja.exemplo.com/fone"));

        assertThat(presente.getDescricao()).isEmpty();
        assertThat(presente.getCaracteristicas()).isEmpty();
    }

    @Test
    void listaBuscaERemovePresentes() {
        Presente fone = cenario.presentes.cadastrar(dados("https://loja.exemplo.com/fone"));
        Presente caneca = cenario.presentes.cadastrar(new DadosPresente("Caneca", Categoria.CASA, "",
                new BigDecimal("40"), List.of(), List.of(Ocasiao.NATAL), "https://loja.exemplo.com/caneca"));

        assertThat(cenario.presentes.listar()).extracting(Presente::getNome).containsExactly("Fone", "Caneca");

        cenario.presentes.remover(fone.getId());

        assertThat(cenario.presentes.listar()).extracting(Presente::getId).containsExactly(caneca.getId());
        assertThatThrownBy(() -> cenario.presentes.buscarPorId(fone.getId()))
                .isInstanceOf(PresenteNaoEncontradoException.class);
        assertThatThrownBy(() -> cenario.presentes.remover(fone.getId()))
                .isInstanceOf(PresenteNaoEncontradoException.class);
    }

    @Test
    void recusaDadosObrigatoriosAusentesOuInvalidos() {
        String link = "https://loja.exemplo.com/fone";
        assertThatThrownBy(() -> cenario.presentes.cadastrar(new DadosPresente(" ", Categoria.JOGOS, "",
                BigDecimal.TEN, List.of(), List.of(Ocasiao.NATAL), link)))
                .isInstanceOf(ValidacaoException.class).hasMessage("O nome do presente é obrigatório.");
        assertThatThrownBy(() -> cenario.presentes.cadastrar(new DadosPresente("Jogo", null, "",
                BigDecimal.TEN, List.of(), List.of(Ocasiao.NATAL), link)))
                .isInstanceOf(ValidacaoException.class).hasMessage("A categoria é obrigatória.");
        assertThatThrownBy(() -> cenario.presentes.cadastrar(new DadosPresente("Jogo", Categoria.JOGOS, "",
                null, List.of(), List.of(Ocasiao.NATAL), link)))
                .isInstanceOf(ValidacaoException.class).hasMessage("O preço é obrigatório.");
        assertThatThrownBy(() -> cenario.presentes.cadastrar(new DadosPresente("Jogo", Categoria.JOGOS, "",
                new BigDecimal("-5"), List.of(), List.of(Ocasiao.NATAL), link)))
                .isInstanceOf(ValidacaoException.class).hasMessage("O preço deve ser maior que zero.");
        assertThatThrownBy(() -> cenario.presentes.cadastrar(new DadosPresente("Jogo", Categoria.JOGOS, "",
                BigDecimal.ZERO, List.of(), List.of(Ocasiao.NATAL), link)))
                .isInstanceOf(ValidacaoException.class).hasMessage("O preço deve ser maior que zero.");
        assertThatThrownBy(() -> cenario.presentes.cadastrar(new DadosPresente("Jogo", Categoria.JOGOS, "",
                BigDecimal.TEN, List.of(), Arrays.asList((Ocasiao) null), link)))
                .isInstanceOf(ValidacaoException.class).hasMessage("Informe ao menos uma ocasião compatível.");
        assertThat(cenario.presentes.listar()).isEmpty();
        assertThat(cenario.linksConsultados).as("dados inválidos não chegam a buscar a imagem").isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Kit 3 em 1", "Fone Bluetooth 5.0", "Caneca \"Melhor Mãe\"", "Lego® Star Wars", "7 Wonders"})
    void aceitaNomesDeProdutoComNumerosESimbolos(String nome) {
        assertThat(cenario.presentes.cadastrar(comNome(nome)).getNome()).isEqualTo(nome);
    }

    @ParameterizedTest
    @ValueSource(strings = {"!!!", "---", "★"})
    void recusaNomeDeProdutoSemLetraNemNumero(String nome) {
        assertThatThrownBy(() -> cenario.presentes.cadastrar(comNome(nome)))
                .isInstanceOf(ValidacaoException.class).hasMessage("O nome deve ter ao menos uma letra ou número.");
    }

    @Test
    void recusaNomeDeProdutoLongoDemaisOuComCaracteresDeControle() {
        assertThat(cenario.presentes.cadastrar(comNome("a".repeat(100))).getNome()).hasSize(100);
        assertThatThrownBy(() -> cenario.presentes.cadastrar(comNome("a".repeat(101))))
                .isInstanceOf(ValidacaoException.class).hasMessage("O nome deve ter no máximo 100 caracteres.");
        assertThatThrownBy(() -> cenario.presentes.cadastrar(comNome("Fo\u0007ne")))
                .isInstanceOf(ValidacaoException.class).hasMessage("O nome contém caracteres inválidos.");
        assertThat(cenario.presentes.cadastrar(comNome("Fone\n\tsem fio")).getNome()).isEqualTo("Fone sem fio");
    }

    private DadosPresente comNome(String nome) {
        return new DadosPresente(nome, Categoria.OUTRA, "", BigDecimal.TEN, List.of(), List.of(Ocasiao.NATAL),
                "https://loja.exemplo.com/produto");
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
