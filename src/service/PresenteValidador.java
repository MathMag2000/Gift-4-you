package service;

import exception.ValidacaoException;
import model.DadosPresente;
import model.Ocasiao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class PresenteValidador extends ValidadorBase {

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

        return new DadosPresente(nome, dados.categoria(), descricao, dados.preco(),
                normalizarLista(dados.caracteristicas()), ocasioes);
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
