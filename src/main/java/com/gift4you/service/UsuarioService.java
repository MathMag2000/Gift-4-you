package com.gift4you.service;

import com.gift4you.exception.CredenciaisInvalidasException;
import com.gift4you.exception.EmailJaCadastradoException;
import com.gift4you.exception.UsuarioNaoEncontradoException;
import com.gift4you.model.DadosCadastroUsuario;
import com.gift4you.model.DadosLogin;
import com.gift4you.model.Usuario;
import com.gift4you.repository.UsuarioRepository;

import java.util.Optional;

public class UsuarioService {

    private final UsuarioRepository repository;
    private final UsuarioValidador validador;
    private final CodificadorSenha codificador;
    /**
     * Hash conferido quando o e-mail não existe, para que o login demore o mesmo tempo nos dois casos
     * e não revele quais e-mails têm conta.
     */
    private final String hashFicticio;

    public UsuarioService(UsuarioRepository repository, UsuarioValidador validador, CodificadorSenha codificador) {
        this.repository = repository;
        this.validador = validador;
        this.codificador = codificador;
        this.hashFicticio = codificador.codificar("senha-ficticia-0");
    }

    public Usuario cadastrar(DadosCadastroUsuario dados) {
        DadosCadastroUsuario validados = validador.validarCadastro(dados);
        if (repository.buscarPorEmail(validados.email()).isPresent()) {
            throw new EmailJaCadastradoException();
        }
        return repository.salvar(new Usuario(validados.nome(), validados.email(),
                codificador.codificar(validados.senha())));
    }

    /**
     * Confere e-mail e senha. Falhas não informam se o erro foi no e-mail ou na senha.
     */
    public Usuario autenticar(DadosLogin dados) {
        if (dados == null || dados.email() == null || dados.email().isBlank()
                || dados.senha() == null || dados.senha().isEmpty()
                || dados.senha().length() > UsuarioValidador.TAMANHO_MAXIMO_SENHA) {
            throw new CredenciaisInvalidasException();
        }
        Optional<Usuario> usuario = repository.buscarPorEmail(validador.normalizarEmail(dados.email()));
        String hash = usuario.map(Usuario::getSenhaHash).orElse(hashFicticio);
        boolean senhaConfere = codificador.confere(dados.senha(), hash);
        if (usuario.isEmpty() || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }
        return usuario.get();
    }

    public Usuario buscarPorId(int id) {
        return repository.buscarPorId(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }
}
