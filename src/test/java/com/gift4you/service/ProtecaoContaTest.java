package com.gift4you.service;

import com.gift4you.exception.CredenciaisInvalidasException;
import com.gift4you.exception.LimiteExcedidoException;
import com.gift4you.model.DadosCadastroUsuario;
import com.gift4you.model.DadosLogin;
import com.gift4you.repository.UsuarioRepositoryEmMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Limites de tentativas de login e de criação de contas.
 */
class ProtecaoContaTest {

    private static final String IP = "203.0.113.7";

    private final RelogioAjustavel relogio = new RelogioAjustavel();
    private final UsuarioService usuarios = new UsuarioService(new UsuarioRepositoryEmMemoria(), new UsuarioValidador(),
            new CodificadorSenhaPbkdf2(1000), new LimitesConta(
            new LimiteDeTaxa(3, Duration.ofMinutes(15), relogio),
            new LimiteDeTaxa(5, Duration.ofMinutes(15), relogio),
            new LimiteDeTaxa(2, Duration.ofHours(1), relogio)));

    @BeforeEach
    void cadastrarConta() {
        usuarios.cadastrar(new DadosCadastroUsuario("Marcos", "marcos@exemplo.com", "senha1234"), "198.51.100.1");
    }

    private void errarSenha(String email, String origem) {
        assertThatThrownBy(() -> usuarios.autenticar(new DadosLogin(email, "errada123"), origem))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void depoisDeMuitosErrosRecusaAteASenhaCertaPorUmTempo() {
        errarSenha("marcos@exemplo.com", IP);
        errarSenha("marcos@exemplo.com", IP);
        errarSenha("MARCOS@exemplo.com", IP);

        assertThatThrownBy(() -> usuarios.autenticar(new DadosLogin("marcos@exemplo.com", "senha1234"), "outro-ip"))
                .isInstanceOf(LimiteExcedidoException.class)
                .hasMessage("Muitas tentativas de login sem sucesso. Tente novamente em 15 minutos.");

        relogio.avancar(Duration.ofMinutes(15));

        assertThat(usuarios.autenticar(new DadosLogin("marcos@exemplo.com", "senha1234"), IP).getNome())
                .isEqualTo("Marcos");
    }

    @Test
    void loginCertoZeraOsErrosDoEmail() {
        errarSenha("marcos@exemplo.com", IP);
        errarSenha("marcos@exemplo.com", IP);
        usuarios.autenticar(new DadosLogin("marcos@exemplo.com", "senha1234"), IP);

        errarSenha("marcos@exemplo.com", IP);
        errarSenha("marcos@exemplo.com", IP);

        assertThat(usuarios.autenticar(new DadosLogin("marcos@exemplo.com", "senha1234"), IP)).isNotNull();
    }

    @Test
    void bloqueiaOEnderecoQueTestaSenhasEmVariasContas() {
        for (int i = 1; i <= 5; i++) {
            errarSenha("conta" + i + "@exemplo.com", IP);
        }

        assertThatThrownBy(() -> usuarios.autenticar(new DadosLogin("marcos@exemplo.com", "senha1234"), IP))
                .isInstanceOf(LimiteExcedidoException.class);
        assertThat(usuarios.autenticar(new DadosLogin("marcos@exemplo.com", "senha1234"), "outro-ip"))
                .as("outros endereços continuam entrando").isNotNull();
    }

    @Test
    void emailsInexistentesTambemSaoBloqueadosParaNaoRevelarQuaisExistem() {
        errarSenha("ninguem@exemplo.com", IP);
        errarSenha("ninguem@exemplo.com", IP);
        errarSenha("ninguem@exemplo.com", IP);

        assertThatThrownBy(() -> usuarios.autenticar(new DadosLogin("ninguem@exemplo.com", "senha1234"), IP))
                .isInstanceOf(LimiteExcedidoException.class);
    }

    @Test
    void limitaContasCriadasPeloMesmoEndereco() {
        usuarios.cadastrar(new DadosCadastroUsuario("Ana", "ana@exemplo.com", "senha1234"), IP);
        usuarios.cadastrar(new DadosCadastroUsuario("Bia", "bia@exemplo.com", "senha1234"), IP);

        assertThatThrownBy(() -> usuarios.cadastrar(
                new DadosCadastroUsuario("Caio", "caio@exemplo.com", "senha1234"), IP))
                .isInstanceOf(LimiteExcedidoException.class)
                .hasMessageStartingWith("Muitas contas criadas a partir desta conexão.");
        assertThat(usuarios.cadastrar(new DadosCadastroUsuario("Caio", "caio@exemplo.com", "senha1234"), "outro-ip"))
                .isNotNull();
    }

    @Test
    void cadastrosRecusadosPorDadosInvalidosNaoContam() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> usuarios.cadastrar(new DadosCadastroUsuario("Ana", "ana@", "senha1234"), IP))
                    .hasMessageContaining("e-mail");
        }

        assertThat(usuarios.cadastrar(new DadosCadastroUsuario("Ana", "ana@exemplo.com", "senha1234"), IP)).isNotNull();
    }
}
