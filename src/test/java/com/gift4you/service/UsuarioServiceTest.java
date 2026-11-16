package com.gift4you.service;

import com.gift4you.exception.CredenciaisInvalidasException;
import com.gift4you.exception.EmailJaCadastradoException;
import com.gift4you.exception.UsuarioNaoEncontradoException;
import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.DadosCadastroUsuario;
import com.gift4you.model.DadosLogin;
import com.gift4you.model.Usuario;
import com.gift4you.repository.UsuarioRepositoryEmMemoria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioServiceTest {

    private final UsuarioService usuarios = new UsuarioService(new UsuarioRepositoryEmMemoria(),
            new UsuarioValidador(), new CodificadorSenhaPbkdf2(1000));

    private Usuario cadastrar(String email, String senha) {
        return usuarios.cadastrar(new DadosCadastroUsuario("Marcos", email, senha));
    }

    @Test
    void cadastraNormalizandoNomeEEmailEGuardandoSomenteOHashDaSenha() {
        Usuario usuario = usuarios.cadastrar(new DadosCadastroUsuario("  Marcos  ", " Marcos@Exemplo.COM ", "senha1234"));

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getNome()).isEqualTo("Marcos");
        assertThat(usuario.getEmail()).isEqualTo("marcos@exemplo.com");
        assertThat(usuario.getSenhaHash()).startsWith("pbkdf2_sha256$").doesNotContain("senha1234");
    }

    @Test
    void naoPermiteDuasContasComOMesmoEmail() {
        cadastrar("marcos@exemplo.com", "senha1234");

        assertThatThrownBy(() -> cadastrar("MARCOS@exemplo.com", "outra1234"))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "marcos", "marcos@", "@exemplo.com", "marcos@exemplo", "mar cos@exemplo.com",
            "marcos@exemplo.", "marcos@@exemplo.com"})
    void recusaEmailInvalido(String email) {
        assertThatThrownBy(() -> cadastrar(email, "senha1234")).isInstanceOf(ValidacaoException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    void recusaEmailLongoDemais() {
        assertThatThrownBy(() -> cadastrar("a".repeat(250) + "@exemplo.com", "senha1234"))
                .isInstanceOf(ValidacaoException.class);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "curta1     | A senha deve ter pelo menos 8 caracteres.",
            "somenteletras | A senha deve ter letras e números.",
            "12345678   | A senha deve ter letras e números."})
    void recusaSenhaFraca(String senha, String mensagem) {
        assertThatThrownBy(() -> cadastrar("marcos@exemplo.com", senha.strip()))
                .isInstanceOf(ValidacaoException.class).hasMessage(mensagem);
    }

    @Test
    void recusaSenhaAusenteOuLongaDemais() {
        assertThatThrownBy(() -> cadastrar("marcos@exemplo.com", null)).hasMessage("A senha é obrigatória.");
        assertThatThrownBy(() -> cadastrar("marcos@exemplo.com", "a1".repeat(65)))
                .hasMessage("A senha deve ter no máximo 128 caracteres.");
    }

    @Test
    void recusaNomeAusente() {
        assertThatThrownBy(() -> usuarios.cadastrar(new DadosCadastroUsuario(" ", "marcos@exemplo.com", "senha1234")))
                .isInstanceOf(ValidacaoException.class).hasMessage("O nome é obrigatório.");
    }

    @Test
    void autenticaComEmailEmQualquerCaixaESenhaCorreta() {
        Usuario cadastrado = cadastrar("marcos@exemplo.com", "senha1234");

        Usuario autenticado = usuarios.autenticar(new DadosLogin("  MARCOS@exemplo.com ", "senha1234"));

        assertThat(autenticado.getId()).isEqualTo(cadastrado.getId());
    }

    @Test
    void recusaSenhaErradaEmailInexistenteOuDadosVazios() {
        cadastrar("marcos@exemplo.com", "senha1234");

        assertThatThrownBy(() -> usuarios.autenticar(new DadosLogin("marcos@exemplo.com", "Senha1234")))
                .isInstanceOf(CredenciaisInvalidasException.class).hasMessage("E-mail ou senha incorretos.");
        assertThatThrownBy(() -> usuarios.autenticar(new DadosLogin("ninguem@exemplo.com", "senha1234")))
                .isInstanceOf(CredenciaisInvalidasException.class).hasMessage("E-mail ou senha incorretos.");
        assertThatThrownBy(() -> usuarios.autenticar(new DadosLogin(null, null)))
                .isInstanceOf(CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> usuarios.autenticar(null)).isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void buscaPorId() {
        Usuario usuario = cadastrar("marcos@exemplo.com", "senha1234");

        assertThat(usuarios.buscarPorId(usuario.getId()).getEmail()).isEqualTo("marcos@exemplo.com");
        assertThatThrownBy(() -> usuarios.buscarPorId(999)).isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    void dadosDeLoginNaoMostramASenhaAoSeremImpressos() {
        assertThat(new DadosLogin("marcos@exemplo.com", "senha1234").toString()).doesNotContain("senha1234");
        assertThat(new DadosCadastroUsuario("Marcos", "m@x.com", "senha1234").toString()).doesNotContain("senha1234");
    }
}
