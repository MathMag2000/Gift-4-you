package com.gift4you.service;

import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.ItemSugerido;
import com.gift4you.model.OrigemSugestao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.Presente;
import com.gift4you.model.ResultadoIdeias;
import com.gift4you.model.SugestaoPresente;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Gera sugestões priorizando presentes dentro do orçamento, compatíveis com gostos, interesses
 * e ocasião, e descartando os relacionados ao que a pessoa não gosta.
 */
public class SugestaoService {

    private static final int PONTOS_POR_GOSTO = 2;
    private static final int PONTOS_POR_INTERESSE = 2;
    private static final int PONTOS_OCASIAO = 3;
    private static final int LIMITE_SUGESTOES = 5;
    private static final int QUANTIDADE_IDEIAS_IA = 6;

    private final PessoaService pessoaService;
    private final PresenteService presenteService;
    private final ComparadorTermos comparador;
    private final GeradorIdeias geradorIdeias;
    private final HistoricoService historicoService;
    private final LinkBuscaProduto linkBusca;

    public SugestaoService(PessoaService pessoaService, PresenteService presenteService,
                           ComparadorTermos comparador, GeradorIdeias geradorIdeias,
                           HistoricoService historicoService, LinkBuscaProduto linkBusca) {
        this.pessoaService = pessoaService;
        this.presenteService = presenteService;
        this.comparador = comparador;
        this.geradorIdeias = geradorIdeias;
        this.historicoService = historicoService;
        this.linkBusca = linkBusca;
    }

    /** Gera as sugestões do catálogo e as registra no histórico. */
    public List<SugestaoPresente> gerarSugestoes(int pessoaId) {
        Pessoa pessoa = pessoaService.buscarPorId(pessoaId);
        List<SugestaoPresente> sugestoes = presenteService.listar().stream()
                .filter(presente -> dentroDoOrcamento(presente.getPreco(), pessoa.getOrcamento()))
                .filter(presente -> !rejeitado(pessoa, textosDoPresente(presente)))
                .map(presente -> avaliar(pessoa, presente))
                .filter(sugestao -> sugestao.pontuacao() > 0)
                .sorted(Comparator.comparingInt(SugestaoPresente::pontuacao).reversed()
                        .thenComparing(sugestao -> sugestao.presente().getPreco()))
                .limit(LIMITE_SUGESTOES)
                .toList();
        historicoService.registrar(pessoa, OrigemSugestao.CATALOGO,
                sugestoes.stream().map(ItemSugerido::de).toList());
        return sugestoes;
    }

    /**
     * Pede ideias à IA e aplica as mesmas regras de orçamento e rejeição do catálogo,
     * pois o modelo nem sempre respeita as restrições pedidas. As ideias aprovadas recebem um link de busca
     * numa loja e vão para o histórico.
     */
    public ResultadoIdeias gerarIdeiasComIa(int pessoaId) {
        Pessoa pessoa = pessoaService.buscarPorId(pessoaId);
        List<IdeiaPresente> recebidas = geradorIdeias.gerarIdeias(pessoa, QUANTIDADE_IDEIAS_IA);
        List<IdeiaPresente> aprovadas = recebidas.stream()
                .filter(ideia -> dentroDoOrcamento(ideia.precoEstimado(), pessoa.getOrcamento()))
                .filter(ideia -> !rejeitado(pessoa, List.of(ideia.nome(), ideia.categoria())))
                .map(ideia -> ideia.comLinkCompra(linkBusca.linkPara(ideia.nome())))
                .toList();
        historicoService.registrar(pessoa, OrigemSugestao.IA, aprovadas.stream().map(ItemSugerido::de).toList());
        return new ResultadoIdeias(aprovadas, recebidas.size() - aprovadas.size());
    }

    private SugestaoPresente avaliar(Pessoa pessoa, Presente presente) {
        List<String> textos = textosDoPresente(presente);
        List<String> gostos = termosEncontrados(pessoa.getGostos(), textos);
        List<String> interesses = termosEncontrados(pessoa.getInteresses(), textos);
        boolean ocasiaoCompativel = presente.getOcasioes().contains(pessoa.getOcasiao());

        int pontuacao = gostos.size() * PONTOS_POR_GOSTO + interesses.size() * PONTOS_POR_INTERESSE;
        List<String> motivos = new ArrayList<>();
        if (!gostos.isEmpty()) {
            motivos.add("combina com os gostos: " + String.join(", ", gostos));
        }
        if (!interesses.isEmpty()) {
            motivos.add("combina com os interesses: " + String.join(", ", interesses));
        }
        if (ocasiaoCompativel) {
            pontuacao += PONTOS_OCASIAO;
            motivos.add("adequado para " + pessoa.getOcasiao());
        }
        return new SugestaoPresente(presente, pontuacao, motivos);
    }

    private boolean dentroDoOrcamento(BigDecimal preco, FaixaOrcamento orcamento) {
        return preco != null
                && preco.compareTo(orcamento.minimo()) >= 0
                && preco.compareTo(orcamento.maximo()) <= 0;
    }

    private boolean rejeitado(Pessoa pessoa, List<String> textos) {
        return pessoa.getNaoGosta().stream().anyMatch(termo -> comparador.corresponde(termo, textos));
    }

    private List<String> termosEncontrados(List<String> termos, List<String> textos) {
        return termos.stream().filter(termo -> comparador.corresponde(termo, textos)).toList();
    }

    private List<String> textosDoPresente(Presente presente) {
        List<String> textos = new ArrayList<>(presente.getCaracteristicas());
        textos.add(presente.getNome());
        textos.add(presente.getDescricao());
        textos.add(presente.getCategoria().toString());
        return textos;
    }
}
