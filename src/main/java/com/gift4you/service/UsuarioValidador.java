package com.gift4you.service;

import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.DadosCadastroUsuario;

import java.util.Locale;
import java.util.regex.Pattern;

public class UsuarioValidador extends ValidadorBase {

    public static final int TAMANHO_MAXIMO_EMAIL = 254;
    public static final int TAMANHO_MINIMO_SENHA = 8;
    public static final int TAMANHO_MAXIMO_SENHA = 128;

    /** Algo antes e depois do @, e um domínio com ponto, sem espaços. */
    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$");
    private static final Pattern TEM_LETRA = Pattern.compile("\\p{L}");
    private static final Pattern TEM_NUMERO = Pattern.compile("\\p{N}");

    /**
     * Valida os dados do cadastro e devolve uma cópia normalizada
     * (nome sem espaços nas pontas e e-mail em minúsculas). A senha não é alterada.
     */
    public DadosCadastroUsuario validarCadastro(DadosCadastroUsuario dados) {
        String nome = validarNomePessoa(dados.nome(), "O nome é obrigatório.");
        String email = validarEmail(dados.email());
        validarSenha(dados.senha());
        return new DadosCadastroUsuario(nome, email, dados.senha());
    }

    public String validarEmail(String email) {
        String normalizado = normalizarEmail(exigirTexto(email, "O e-mail é obrigatório."));
        if (normalizado.length() > TAMANHO_MAXIMO_EMAIL || !FORMATO_EMAIL.matcher(normalizado).matches()) {
            throw new ValidacaoException("Informe um e-mail válido, como nome@exemplo.com.");
        }
        return normalizado;
    }

    /** E-mails são comparados sem diferenciar maiúsculas, para que "Ana@X.com" e "ana@x.com" sejam a mesma conta. */
    public String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.isEmpty()) {
            throw new ValidacaoException("A senha é obrigatória.");
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new ValidacaoException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
        }
        if (senha.length() > TAMANHO_MAXIMO_SENHA) {
            throw new ValidacaoException("A senha deve ter no máximo " + TAMANHO_MAXIMO_SENHA + " caracteres.");
        }
        if (senha.isBlank() || !TEM_LETRA.matcher(senha).find() || !TEM_NUMERO.matcher(senha).find()) {
            throw new ValidacaoException("A senha deve ter letras e números.");
        }
    }
}
