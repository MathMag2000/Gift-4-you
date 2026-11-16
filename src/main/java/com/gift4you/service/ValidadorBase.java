package com.gift4you.service;

import com.gift4you.exception.ValidacaoException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class ValidadorBase {

    public static final int TAMANHO_MINIMO_NOME_PESSOA = 2;
    public static final int TAMANHO_MAXIMO_NOME = 100;

    /** Letras (com acento), espaços, apóstrofo, hífen e ponto, como em "Ana D'Ávila", "Jean-Luc" ou "João Jr.". */
    private static final Pattern CARACTERES_NOME_PESSOA = Pattern.compile("[\\p{L}\\p{M} .'’-]+");
    private static final Pattern TEM_LETRA = Pattern.compile("\\p{L}");
    private static final Pattern TEM_LETRA_OU_NUMERO = Pattern.compile("[\\p{L}\\p{N}]");
    private static final Pattern CARACTERE_DE_CONTROLE = Pattern.compile("\\p{Cntrl}");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    /**
     * Valida o nome de uma pessoa (ou de um usuário) e o devolve sem espaços nas pontas e sem espaços repetidos.
     *
     * @param obrigatorio mensagem usada quando o nome não é informado
     */
    protected String validarNomePessoa(String nome, String obrigatorio) {
        String normalizado = normalizarEspacos(exigirTexto(nome, obrigatorio));
        validarTamanho(normalizado, TAMANHO_MINIMO_NOME_PESSOA);
        if (!CARACTERES_NOME_PESSOA.matcher(normalizado).matches() || !TEM_LETRA.matcher(normalizado).find()) {
            throw new ValidacaoException(
                    "O nome deve conter apenas letras, espaços, apóstrofo, hífen ou ponto.");
        }
        return normalizado;
    }

    /**
     * Valida o nome de um produto. Números e símbolos são aceitos (ex.: "Kit 3 em 1", "Fone Bluetooth 5.0"),
     * mas o nome precisa ter ao menos uma letra ou número e não pode ter caracteres de controle.
     */
    protected String validarNomeProduto(String nome, String obrigatorio) {
        // Quebras de linha e tabulações viram espaços; outros caracteres de controle são recusados.
        String normalizado = normalizarEspacos(exigirTexto(nome, obrigatorio));
        if (CARACTERE_DE_CONTROLE.matcher(normalizado).find()) {
            throw new ValidacaoException("O nome contém caracteres inválidos.");
        }
        validarTamanho(normalizado, 1);
        if (!TEM_LETRA_OU_NUMERO.matcher(normalizado).find()) {
            throw new ValidacaoException("O nome deve ter ao menos uma letra ou número.");
        }
        return normalizado;
    }

    private void validarTamanho(String nome, int minimo) {
        if (nome.length() < minimo) {
            throw new ValidacaoException("O nome deve ter pelo menos " + minimo + " caracteres.");
        }
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            throw new ValidacaoException("O nome deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
        }
    }

    private String normalizarEspacos(String texto) {
        return ESPACOS.matcher(texto.trim()).replaceAll(" ");
    }

    protected String exigirTexto(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoException(mensagem);
        }
        return valor.trim();
    }

    protected void exigir(Object valor, String mensagem) {
        if (valor == null) {
            throw new ValidacaoException(mensagem);
        }
    }

    /**
     * Remove espaços nas pontas, converte para minúsculas e descarta itens vazios ou repetidos.
     */
    protected List<String> normalizarLista(List<String> itens) {
        if (itens == null) {
            return List.of();
        }
        List<String> normalizados = new ArrayList<>();
        itens.stream()
                .filter(Objects::nonNull)
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .filter(item -> !item.isEmpty() && !normalizados.contains(item))
                .forEach(normalizados::add);
        return normalizados;
    }
}
