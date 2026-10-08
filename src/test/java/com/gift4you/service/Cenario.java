package com.gift4you.service;

import com.gift4you.integracao.BuscaAmazon;
import com.gift4you.model.Categoria;
import com.gift4you.model.DadosPessoa;
import com.gift4you.model.DadosPresente;
import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.Presente;
import com.gift4you.model.Vinculo;
import com.gift4you.repository.FavoritoRepositoryEmMemoria;
import com.gift4you.repository.HistoricoRepositoryEmMemoria;
import com.gift4you.repository.PessoaRepositoryEmMemoria;
import com.gift4you.repository.PresenteRepositoryEmMemoria;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Monta os services ligados entre si, com relógio fixo e uma IA falsa que devolve {@link #ideiasDaIa}.
 */
class Cenario {

    final Clock relogio = Clock.fixed(Instant.parse("2026-10-08T12:00:00Z"), ZoneOffset.UTC);
    final List<IdeiaPresente> ideiasDaIa = new ArrayList<>();
    /** Links que o extrator falso recebeu. Links com "sem-imagem" simulam uma loja que bloqueia a leitura. */
    final List<String> linksConsultados = new ArrayList<>();

    final PessoaService pessoas = new PessoaService(new PessoaRepositoryEmMemoria(), new PessoaValidador());
    final PresenteService presentes = new PresenteService(new PresenteRepositoryEmMemoria(), new PresenteValidador(),
            link -> {
                linksConsultados.add(link);
                return link.contains("sem-imagem") ? Optional.empty() : Optional.of(link + "/foto.jpg");
            });
    final HistoricoService historico = new HistoricoService(new HistoricoRepositoryEmMemoria(), relogio);
    final LinkBuscaProduto linkBusca = new BuscaAmazon();
    final FavoritoService favoritos = new FavoritoService(new FavoritoRepositoryEmMemoria(), new FavoritoValidador(),
            pessoas, presentes, linkBusca, relogio);
    final SugestaoService sugestoes = new SugestaoService(pessoas, presentes, new ComparadorTermos(),
            (pessoa, quantidade) -> ideiasDaIa, historico, linkBusca);

    Cenario() {
        pessoas.adicionarOuvinteRemocao(historico);
        pessoas.adicionarOuvinteRemocao(favoritos);
    }

    Pessoa cadastrarPessoa(String nome, List<String> gostos, List<String> naoGosta) {
        return pessoas.cadastrar(new DadosPessoa(nome, 30, Vinculo.AMIGO, gostos, List.of(), naoGosta,
                Ocasiao.ANIVERSARIO, new FaixaOrcamento(new BigDecimal("50"), new BigDecimal("200"))));
    }

    Presente cadastrarPresente(String nome, Categoria categoria, String preco, List<String> caracteristicas) {
        return presentes.cadastrar(new DadosPresente(nome, categoria, "", new BigDecimal(preco), caracteristicas,
                List.of(Ocasiao.ANIVERSARIO), linkDe(nome)));
    }

    static String linkDe(String nome) {
        return "https://loja.exemplo.com/" + nome.toLowerCase().replace(' ', '-');
    }

    static IdeiaPresente ideia(String nome, String preco) {
        return new IdeiaPresente(nome, "Diversos", new BigDecimal(preco), "Motivo de " + nome);
    }
}
