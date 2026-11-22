package com.gift4you.service;

import com.gift4you.exception.CredenciaisInvalidasException;
import com.gift4you.exception.LimiteExcedidoException;
import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.DadosAlteracaoSenha;
import com.gift4you.model.DadosCadastroUsuario;
import com.gift4you.model.DadosLogin;
import com.gift4you.model.Usuario;
import com.gift4you.repository.UsuarioRepositoryEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Alteração do nome e da senha da conta, na tela de configurações. */
class AlteracaoContaTest {

    private final Clock relogio = Clock.systemUTC();
    private final UsuarioService usuarios = new UsuarioService(new UsuarioRepositoryEmMemoria(), new UsuarioValidador(),
            new CodificadorSenhaPbkdf2(1000), new LimitesConta(new LimiteDeTaxa(3, Duration.ofMinutes(15), relogio),
            new LimiteDeTaxa(100, Duration.ofMinutes(15), relogio), new LimiteDeTaxa(100, Duration.ofHours(1), relogio)));
    private Usuario marcos;

    @BeforeEach
    void cadastrar() {
        marcos = usuarios.cadastrar(new DadosCadastroUsuario("Marcos", "marcos@exemplo.com", "senha1234"));
    }

    @Test
    void alteraONomeNormalizando() {
        usuarios.alterarNome(marcos.getId(), "  Marcos   Júnior ");

        assertThat(usuarios.buscarPorId(marcos.getId()).getNome()).isEqualTo("Marcos Júnior");
    }

    @Test
    void recusaNomeInvalido() {
        assertThatThrownBy(() -> usuarios.alterarNome(marcos.getId(), " ")).isInstanceOf(ValidacaoException.class)
                .hasMessage("O nome é obrigatório.");
        assertThatThrownBy(() -> usuarios.alterarNome(marcos.getId(), "Marcos 123"))
                .isInstanceOf(ValidacaoException.class);
        assertThat(usuarios.buscarPorId(marcos.getId()).getNome()).isEqualTo("Marcos");
    }

    @Test
    void alteraASenhaEOLoginPassaAUsarANova() {
        usuarios.alterarSenha(marcos.getId(), new DadosAlteracaoSenha("senha1234", "novaSenha99"));

        assertThat(usuarios.autenticar(new DadosLogin("marcos@exemplo.com", "novaSenha99")).getId())
                .isEqualTo(marcos.getId());
        assertThatThrownBy(() -> usuarios.autenticar(new DadosLogin("marcos@exemplo.com", "senha1234")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void exigeASenhaAtualCorreta() {
        assertThatThrownBy(() -> usuarios.alterarSenha(marcos.getId(), new DadosAlteracaoSenha("errada123", "novaSenha99")))
                .isInstanceOf(ValidacaoException.class).hasMessage("A senha atual está incorreta.");
        assertThatThrownBy(() -> usuarios.alterarSenha(marcos.getId(), new DadosAlteracaoSenha("", "novaSenha99")))
                .isInstanceOf(ValidacaoException.class).hasMessage("Informe a senha atual.");
    }

    @Test
    void novaSenhaSegueAsRegrasDoCadastroEPrecisaSerDiferente() {
        assertThatThrownBy(() -> usuarios.alterarSenha(marcos.getId(), new DadosAlteracaoSenha("senha1234", "curta1")))
                .isInstanceOf(ValidacaoException.class).hasMessageContaining("8 caracteres");
        assertThatThrownBy(() -> usuarios.alterarSenha(marcos.getId(), new DadosAlteracaoSenha("senha1234", "semnumeros")))
                .isInstanceOf(ValidacaoException.class).hasMessage("A senha deve ter letras e números.");
        assertThatThrownBy(() -> usuarios.alterarSenha(marcos.getId(), new DadosAlteracaoSenha("senha1234", "senha1234")))
                .isInstanceOf(ValidacaoException.class).hasMessage("A nova senha deve ser diferente da atual.");
    }

    @Test
    void muitasTentativasComASenhaAtualErradaBloqueiamPorUmTempo() {
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> usuarios.alterarSenha(marcos.getId(),
                    new DadosAlteracaoSenha("errada123", "novaSenha99"))).isInstanceOf(ValidacaoException.class);
        }

        assertThatThrownBy(() -> usuarios.alterarSenha(marcos.getId(), new DadosAlteracaoSenha("senha1234", "novaSenha99")))
                .isInstanceOf(LimiteExcedidoException.class);
    }
}
