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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    private final LojaOnline loja;

    public SugestaoService(PessoaService pessoaService, PresenteService presenteService,
                           ComparadorTermos comparador, GeradorIdeias geradorIdeias,
                           HistoricoService historicoService, LojaOnline loja) {
        this.pessoaService = pessoaService;
        this.presenteService = presenteService;
        this.comparador = comparador;
        this.geradorIdeias = geradorIdeias;
        this.historicoService = historicoService;
        this.loja = loja;
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
     * pois o modelo nem sempre respeita as restrições pedidas. Cada ideia aprovada é procurada na loja
     * para ganhar o link da página do produto, e o resultado vai para o histórico.
     */
    public ResultadoIdeias gerarIdeiasComIa(int pessoaId) {
        Pessoa pessoa = pessoaService.buscarPorId(pessoaId);
        List<IdeiaPresente> recebidas = geradorIdeias.gerarIdeias(pessoa, QUANTIDADE_IDEIAS_IA);
        List<IdeiaPresente> aprovadas = recebidas.stream()
                .filter(ideia -> dentroDoOrcamento(ideia.precoEstimado(), pessoa.getOrcamento()))
                .filter(ideia -> !rejeitado(pessoa, List.of(ideia.nome(), ideia.categoria())))
                .toList();
        List<IdeiaPresente> naLoja = procurarNaLoja(pessoa, aprovadas);
        historicoService.registrar(pessoa, OrigemSugestao.IA, naLoja.stream().map(ItemSugerido::de).toList());
        return new ResultadoIdeias(naLoja, recebidas.size() - aprovadas.size());
    }

    /** Procura as ideias ao mesmo tempo, para que o tempo total seja o da busca mais lenta, e não a soma. */
    private List<IdeiaPresente> procurarNaLoja(Pessoa pessoa, List<IdeiaPresente> ideias) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<IdeiaPresente>> buscas = ideias.stream()
                    .map(ideia -> executor.submit(() -> procurarNaLoja(pessoa, ideia)))
                    .toList();
            List<IdeiaPresente> resultado = new ArrayList<>();
            for (int i = 0; i < ideias.size(); i++) {
                resultado.add(aguardar(buscas.get(i), ideias.get(i)));
            }
            return resultado;
        }
    }

    /**
     * Usa o primeiro produto da busca com preço dentro do orçamento e que não seja algo que a pessoa
     * não gosta, assumindo o nome e o preço dele; sem produto, a ideia fica com o link de busca.
     */
    private IdeiaPresente procurarNaLoja(Pessoa pessoa, IdeiaPresente ideia) {
        return loja.encontrarProdutos(ideia.nome()).stream()
                .filter(produto -> dentroDoOrcamento(produto.preco(), pessoa.getOrcamento()))
                .filter(produto -> !rejeitado(pessoa, List.of(produto.titulo())))
                .findFirst()
                .map(ideia::comProduto)
                .orElseGet(() -> semProduto(ideia));
    }

    private IdeiaPresente aguardar(Future<IdeiaPresente> busca, IdeiaPresente ideia) {
        try {
            return busca.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return semProduto(ideia);
        } catch (ExecutionException e) {
            return semProduto(ideia);
        }
    }

    private IdeiaPresente semProduto(IdeiaPresente ideia) {
        return ideia.comLinkBusca(loja.linkBusca(ideia.nome()));
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
        return preco != null && preco.compareTo(orcamento.maximo()) <= 0;
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
