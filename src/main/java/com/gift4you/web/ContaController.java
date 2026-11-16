package com.gift4you.web;

import com.gift4you.exception.NaoAutenticadoException;
import com.gift4you.exception.RegistroNaoEncontradoException;
import com.gift4you.model.DadosCadastroUsuario;
import com.gift4you.model.DadosLogin;
import com.gift4you.model.Usuario;
import com.gift4you.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro, login e logout. São as únicas rotas da API, além das opções, acessíveis sem login.
 */
@RestController
@RequestMapping("/api/conta")
public class ContaController {

    /** Dados da conta enviados ao site: nunca inclui o hash da senha. */
    public record Conta(int id, String nome, String email) {

        static Conta de(Usuario usuario) {
            return new Conta(usuario.getId(), usuario.getNome(), usuario.getEmail());
        }
    }

    private final UsuarioService usuarioService;

    public ContaController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Usuário logado, ou 401 se não houver; o site usa para decidir se mostra a tela de login. */
    @GetMapping
    public Conta atual(HttpServletRequest requisicao) {
        UsuarioLogado logado = SessaoUsuario.usuarioLogado(requisicao).orElseThrow(NaoAutenticadoException::new);
        try {
            return Conta.de(usuarioService.buscarPorId(logado.id()));
        } catch (RegistroNaoEncontradoException e) {
            // A conta da sessão não existe mais (ex.: o banco foi apagado).
            SessaoUsuario.sair(requisicao);
            throw new NaoAutenticadoException();
        }
    }

    /** Cadastra a conta e já entra nela. */
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public Conta cadastrar(@RequestBody DadosCadastroUsuario dados, HttpServletRequest requisicao) {
        Usuario usuario = usuarioService.cadastrar(dados);
        SessaoUsuario.entrar(requisicao, usuario);
        return Conta.de(usuario);
    }

    @PostMapping("/login")
    public Conta entrar(@RequestBody DadosLogin dados, HttpServletRequest requisicao) {
        Usuario usuario = usuarioService.autenticar(dados);
        SessaoUsuario.entrar(requisicao, usuario);
        return Conta.de(usuario);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sair(HttpServletRequest requisicao) {
        SessaoUsuario.sair(requisicao);
    }
}
