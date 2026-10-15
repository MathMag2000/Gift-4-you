package com.gift4you.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifica se um termo informado pelo usuário (ex.: "filmes de terror") aparece em textos,
 * ignorando acentos, maiúsculas, plural simples e palavras sem significado próprio.
 */
public class ComparadorTermos {

    private static final Set<String> PALAVRAS_IGNORADAS = Set.of(
            "a", "o", "as", "os", "um", "uma", "de", "da", "do", "das", "dos",
            "e", "em", "no", "na", "nos", "nas", "com", "para", "por");

    public boolean corresponde(String termo, Collection<String> textos) {
        Set<String> palavrasDoTermo = extrairPalavras(termo);
        if (palavrasDoTermo.isEmpty()) {
            return false;
        }
        Set<String> palavrasDosTextos = textos.stream()
                .flatMap(texto -> extrairPalavras(texto).stream())
                .collect(Collectors.toSet());
        return palavrasDosTextos.containsAll(palavrasDoTermo);
    }

    private Set<String> extrairPalavras(String texto) {
        if (texto == null) {
            return Set.of();
        }
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return Arrays.stream(semAcentos.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(palavra -> !palavra.isEmpty() && !PALAVRAS_IGNORADAS.contains(palavra))
                .map(this::singular)
                .collect(Collectors.toSet());
    }

    private String singular(String palavra) {
        return palavra.length() > 3 && palavra.endsWith("s") ? palavra.substring(0, palavra.length() - 1) : palavra;
    }
}
