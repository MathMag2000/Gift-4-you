package service;

import exception.ValidacaoException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class ValidadorBase {

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
