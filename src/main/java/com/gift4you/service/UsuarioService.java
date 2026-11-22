package com.gift4you.service;

import com.gift4you.exception.CredenciaisInvalidasException;
import com.gift4you.exception.EmailJaCadastradoException;
import com.gift4you.exception.LimiteExcedidoException;
import com.gift4you.exception.UsuarioNaoEncontradoException;
import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.DadosAlteracaoSenha;
import com.gift4you.model.DadosCadastroUsuario;
import com.gift4you.model.DadosLogin;
import com.gift4you.model.Usuario;
import com.gift4you.repository.UsuarioRepository;

import java.time.Clock;
import java.util.Optional;

public class UsuarioService {

    /** Origem usada quando quem chama não informa o endereço de quem fez o pedido (ex.: testes). */
    private static final String ORIGEM_LOCAL = "local";

    private final UsuarioRepository repository;
    private final UsuarioValidador validador;
    private final CodificadorSenha codificador;
    private final LimitesConta limites;
    /**
     * Hash conferido quando o e-mail não existe, para que o login demore o mesmo tempo nos dois casos
     * e não revele quais e-mails têm conta.
     */
    private final String hashFicticio;

    public UsuarioService(UsuarioRepository repository, UsuarioValidador validador, CodificadorSenha codificador) {
        this(repository, validador, codificador, LimitesConta.padrao(Clock.systemUTC()));
    }

    public UsuarioService(UsuarioRepository repository, UsuarioValidador validador, CodificadorSenha codificador,
                          LimitesConta limites) {
        this.repository = repository;
        this.validador = validador;
        this.codificador = codificador;
        this.limites = limites;
        this.hashFicticio = codificador.codificar("senha-ficticia-0");
    }

    public Usuario cadastrar(DadosCadastroUsuario dados) {
        return cadastrar(dados, ORIGEM_LOCAL);
    }

    /**
     * @param origem endereço de quem fez o pedido, para limitar contas criadas em massa
     */
    public Usuario cadastrar(DadosCadastroUsuario dados, String origem) {
        String chaveOrigem = "cadastro:" + origem;
        if (limites.cadastrosPorOrigem().atingido(chaveOrigem)) {
            throw new LimiteExcedidoException("Muitas contas criadas a partir desta conexão. Tente novamente em "
                    + LimiteDeTaxa.emMinutos(limites.cadastrosPorOrigem().tempoParaLiberar(chaveOrigem)) + ".",
                    limites.cadastrosPorOrigem().tempoParaLiberar(chaveOrigem));
        }
        DadosCadastroUsuario validados = validador.validarCadastro(dados);
        if (repository.buscarPorEmail(validados.email()).isPresent()) {
            throw new EmailJaCadastradoException();
        }
        Usuario usuario = repository.salvar(new Usuario(validados.nome(), validados.email(),
                codificador.codificar(validados.senha())));
        limites.cadastrosPorOrigem().registrar(chaveOrigem);
        return usuario;
    }

    public Usuario autenticar(DadosLogin dados) {
        return autenticar(dados, ORIGEM_LOCAL);
    }

    /**
     * Confere e-mail e senha. Falhas não informam se o erro foi no e-mail ou na senha. Depois de muitas
     * falhas seguidas no mesmo e-mail, ou vindas do mesmo endereço, novas tentativas são recusadas por um
     * tempo, mesmo com a senha certa: isso impede descobrir a senha testando muitas combinações.
     *
     * @param origem endereço de quem fez o pedido
     */
    public Usuario autenticar(DadosLogin dados, String origem) {
        String email = dados == null ? "" : validador.normalizarEmail(dados.email());
        String chaveEmail = "login:" + email;
        String chaveOrigem = "login-origem:" + origem;
        recusarSeBloqueado(limites.falhasPorEmail(), chaveEmail);
        recusarSeBloqueado(limites.falhasPorOrigem(), chaveOrigem);

        if (email.isEmpty() || dados.senha() == null || dados.senha().isEmpty()
                || dados.senha().length() > UsuarioValidador.TAMANHO_MAXIMO_SENHA) {
            throw new CredenciaisInvalidasException();
        }
        Optional<Usuario> usuario = repository.buscarPorEmail(email);
        String hash = usuario.map(Usuario::getSenhaHash).orElse(hashFicticio);
        boolean senhaConfere = codificador.confere(dados.senha(), hash);
        if (usuario.isEmpty() || !senhaConfere) {
            limites.falhasPorEmail().registrar(chaveEmail);
            limites.falhasPorOrigem().registrar(chaveOrigem);
            throw new CredenciaisInvalidasException();
        }
        limites.falhasPorEmail().limpar(chaveEmail);
        return usuario.get();
    }

    public Usuario buscarPorId(int id) {
        return repository.buscarPorId(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    public Usuario alterarNome(int usuarioId, String nome) {
        Usuario usuario = buscarPorId(usuarioId);
        usuario.alterarNome(validador.validarNome(nome));
        return repository.atualizar(usuario);
    }

    /**
     * Troca a senha depois de conferir a atual. Erros na senha atual contam como falhas de login do e-mail
     * da conta, para que esta tela não sirva para testar senhas sem limite.
     */
    public Usuario alterarSenha(int usuarioId, DadosAlteracaoSenha dados) {
        Usuario usuario = buscarPorId(usuarioId);
        String chaveEmail = "login:" + usuario.getEmail();
        recusarSeBloqueado(limites.falhasPorEmail(), chaveEmail);

        String senhaAtual = dados == null ? null : dados.senhaAtual();
        if (senhaAtual == null || senhaAtual.isEmpty()) {
            throw new ValidacaoException("Informe a senha atual.");
        }
        if (senhaAtual.length() > UsuarioValidador.TAMANHO_MAXIMO_SENHA
                || !codificador.confere(senhaAtual, usuario.getSenhaHash())) {
            limites.falhasPorEmail().registrar(chaveEmail);
            throw new ValidacaoException("A senha atual está incorreta.");
        }
        limites.falhasPorEmail().limpar(chaveEmail);

        validador.validarSenha(dados.novaSenha());
        if (dados.novaSenha().equals(senhaAtual)) {
            throw new ValidacaoException("A nova senha deve ser diferente da atual.");
        }
        usuario.alterarSenhaHash(codificador.codificar(dados.novaSenha()));
        return repository.atualizar(usuario);
    }

    private void recusarSeBloqueado(LimiteDeTaxa limite, String chave) {
        if (limite.atingido(chave)) {
            throw new LimiteExcedidoException("Muitas tentativas de login sem sucesso. Tente novamente em "
                    + LimiteDeTaxa.emMinutos(limite.tempoParaLiberar(chave)) + ".", limite.tempoParaLiberar(chave));
        }
    }
}
