package com.gift4you.integracao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor e escritor mínimos de JSON, suficientes para conversar com a API do Gemini
 * sem adicionar bibliotecas ao projeto.
 * Objetos viram {@link Map}, arrays viram {@link List} e números viram {@link BigDecimal}.
 */
public final class Json {

    private final String texto;
    private int posicao;

    private Json(String texto) {
        this.texto = texto;
    }

    public static Object ler(String texto) {
        Json leitor = new Json(texto);
        Object valor = leitor.lerValor();
        leitor.pularEspacos();
        if (leitor.posicao != texto.length()) {
            throw leitor.erro("conteúdo inesperado após o fim do JSON");
        }
        return valor;
    }

    public static String escrever(String valor) {
        StringBuilder resultado = new StringBuilder("\"");
        for (char caractere : valor.toCharArray()) {
            switch (caractere) {
                case '"' -> resultado.append("\\\"");
                case '\\' -> resultado.append("\\\\");
                case '\n' -> resultado.append("\\n");
                case '\r' -> resultado.append("\\r");
                case '\t' -> resultado.append("\\t");
                default -> {
                    if (caractere < 0x20) {
                        resultado.append(String.format("\\u%04x", (int) caractere));
                    } else {
                        resultado.append(caractere);
                    }
                }
            }
        }
        return resultado.append('"').toString();
    }

    private Object lerValor() {
        pularEspacos();
        if (posicao >= texto.length()) {
            throw erro("fim inesperado");
        }
        char atual = texto.charAt(posicao);
        return switch (atual) {
            case '{' -> lerObjeto();
            case '[' -> lerLista();
            case '"' -> lerTexto();
            case 't' -> lerLiteral("true", Boolean.TRUE);
            case 'f' -> lerLiteral("false", Boolean.FALSE);
            case 'n' -> lerLiteral("null", null);
            default -> lerNumero();
        };
    }

    private Map<String, Object> lerObjeto() {
        Map<String, Object> objeto = new LinkedHashMap<>();
        esperar('{');
        pularEspacos();
        if (consumirSe('}')) {
            return objeto;
        }
        do {
            pularEspacos();
            String chave = lerTexto();
            pularEspacos();
            esperar(':');
            objeto.put(chave, lerValor());
            pularEspacos();
        } while (consumirSe(','));
        esperar('}');
        return objeto;
    }

    private List<Object> lerLista() {
        List<Object> lista = new ArrayList<>();
        esperar('[');
        pularEspacos();
        if (consumirSe(']')) {
            return lista;
        }
        do {
            lista.add(lerValor());
            pularEspacos();
        } while (consumirSe(','));
        esperar(']');
        return lista;
    }

    private String lerTexto() {
        esperar('"');
        StringBuilder resultado = new StringBuilder();
        while (posicao < texto.length()) {
            char atual = texto.charAt(posicao++);
            if (atual == '"') {
                return resultado.toString();
            }
            if (atual != '\\') {
                resultado.append(atual);
                continue;
            }
            if (posicao >= texto.length()) {
                break;
            }
            char escape = texto.charAt(posicao++);
            switch (escape) {
                case 'n' -> resultado.append('\n');
                case 'r' -> resultado.append('\r');
                case 't' -> resultado.append('\t');
                case 'b' -> resultado.append('\b');
                case 'f' -> resultado.append('\f');
                case 'u' -> {
                    if (posicao + 4 > texto.length()) {
                        throw erro("escape unicode incompleto");
                    }
                    resultado.append((char) Integer.parseInt(texto.substring(posicao, posicao + 4), 16));
                    posicao += 4;
                }
                default -> resultado.append(escape);
            }
        }
        throw erro("texto não finalizado");
    }

    private BigDecimal lerNumero() {
        int inicio = posicao;
        while (posicao < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(posicao)) >= 0) {
            posicao++;
        }
        try {
            return new BigDecimal(texto.substring(inicio, posicao));
        } catch (NumberFormatException e) {
            throw erro("valor inválido");
        }
    }

    private Object lerLiteral(String literal, Object valor) {
        if (!texto.startsWith(literal, posicao)) {
            throw erro("valor inválido");
        }
        posicao += literal.length();
        return valor;
    }

    private void pularEspacos() {
        while (posicao < texto.length() && Character.isWhitespace(texto.charAt(posicao))) {
            posicao++;
        }
    }

    private void esperar(char esperado) {
        if (!consumirSe(esperado)) {
            throw erro("esperado '" + esperado + "'");
        }
    }

    private boolean consumirSe(char esperado) {
        if (posicao < texto.length() && texto.charAt(posicao) == esperado) {
            posicao++;
            return true;
        }
        return false;
    }

    private IllegalArgumentException erro(String mensagem) {
        return new IllegalArgumentException("JSON inválido na posição " + posicao + ": " + mensagem);
    }
}
