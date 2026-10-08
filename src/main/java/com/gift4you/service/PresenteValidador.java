package com.gift4you.service;

import com.gift4you.exception.ValidacaoException;
import com.gift4you.model.DadosPresente;
import com.gift4you.model.Ocasiao;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

public class PresenteValidador extends ValidadorBase {

    private static final int TAMANHO_MAXIMO_LINK = 2000;

    /**
     * Valida os dados informados e devolve uma cópia normalizada
     * (textos sem espaços nas pontas, características em minúsculas e ocasiões sem repetição).
     */
    public DadosPresente validar(DadosPresente dados) {
        String nome = exigirTexto(dados.nome(), "O nome do presente é obrigatório.");
        exigir(dados.categoria(), "A categoria é obrigatória.");
        validarPreco(dados.preco());
        List<Ocasiao> ocasioes = validarOcasioes(dados.ocasioes());
        String descricao = dados.descricao() == null ? "" : dados.descricao().trim();
        String linkCompra = validarLinkCompra(dados.linkCompra());

        return new DadosPresente(nome, dados.categoria(), descricao, dados.preco(),
                normalizarLista(dados.caracteristicas()), ocasioes, linkCompra);
    }

    /** Aceita somente endereços http ou https completos, o que também evita links como "javascript:". */
    private String validarLinkCompra(String link) {
        String texto = exigirTexto(link, "O link para compra é obrigatório.");
        if (texto.length() > TAMANHO_MAXIMO_LINK) {
            throw new ValidacaoException("O link para compra é longo demais.");
        }
        try {
            URI endereco = new URI(texto);
            String esquema = endereco.getScheme();
            if (esquema != null && (esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))
                    && endereco.getHost() != null) {
                return texto;
            }
        } catch (URISyntaxException e) {
            // Tratado abaixo como link inválido.
        }
        throw new ValidacaoException("Informe um link para compra válido, começando com http:// ou https://.");
    }

    private void validarPreco(BigDecimal preco) {
        exigir(preco, "O preço é obrigatório.");
        if (preco.signum() <= 0) {
            throw new ValidacaoException("O preço deve ser maior que zero.");
        }
    }

    private List<Ocasiao> validarOcasioes(List<Ocasiao> ocasioes) {
        List<Ocasiao> distintas = ocasioes == null ? List.of()
                : ocasioes.stream().filter(Objects::nonNull).distinct().sorted().toList();
        if (distintas.isEmpty()) {
            throw new ValidacaoException("Informe ao menos uma ocasião compatível.");
        }
        return distintas;
    }
}
