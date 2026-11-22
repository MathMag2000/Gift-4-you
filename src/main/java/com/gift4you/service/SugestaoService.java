package com.gift4you.service;

import com.gift4you.exception.LimiteExcedidoException;
import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.IdeiaPresente;
import com.gift4you.model.ItemSugerido;
import com.gift4you.model.OrigemSugestao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.Presente;
import com.gift4you.model.ResultadoIdeias;
import com.gift4you.model.SugestaoPresente;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Gera sugestões priorizando presentes dentro do orçamento, compatíveis com os gostos e interesses
 * e com a ocasião, e descartando os relacionados ao que a pessoa não gosta.
 */
public class SugestaoService {

    private static final int PONTOS_POR_GOSTO = 2;
    private static final int PONTOS_OCASIAO = 3;
    private static final int LIMITE_SUGESTOES = 5;
    private static final int QUANTIDADE_IDEIAS_IA = 6;

    private final PessoaService pessoaService;
    private final PresenteService presenteService;
    private final ComparadorTermos comparador;
    private final GeradorIdeias geradorIdeias;
    private final HistoricoService historicoService;
    private final LojaOnline loja;
    /** Pedidos de ideias à IA por conta, pois cada pedido usa a cota (e pode ter custo) da chave do Gemini. */
    private final LimiteDeTaxa limiteIa;

    public SugestaoService(PessoaService pessoaService, PresenteService presenteService,
                           ComparadorTermos comparador, GeradorIdeias geradorIdeias,
                           HistoricoService historicoService, LojaOnline loja, LimiteDeTaxa limiteIa) {
        this.pessoaService = pessoaService;
        this.presenteService = presenteService;
        this.comparador = comparador;
        this.geradorIdeias = geradorIdeias;
        this.historicoService = historicoService;
        this.loja = loja;
        this.limiteIa = limiteIa;
    }

    /**
     * Sugestões do catálogo para a pessoa. Os presentes cadastrados para ela própria ficam de fora, pois
     * aparecem em {@link #presentesDaPessoa}. Consultar sugestões não gera registro no histórico, que guarda
     * somente as ideias geradas com IA.
     */
    public List<SugestaoPresente> gerarSugestoes(int usuarioId, int pessoaId) {
        Pessoa pessoa = pessoaService.buscarPorId(usuarioId, pessoaId);
        return presenteService.listar(usuarioId).stream()
                .filter(presente -> !presente.cadastradoPara(pessoaId))
                .filter(presente -> dentroDoOrcamento(presente.getPreco(), pessoa.getOrcamento()))
                .filter(presente -> !rejeitado(pessoa, textosDoPresente(presente)))
                .map(presente -> avaliar(pessoa, presente))
                .filter(sugestao -> sugestao.pontuacao() > 0)
                .sorted(Comparator.comparingInt(SugestaoPresente::pontuacao).reversed()
                        .thenComparing(sugestao -> sugestao.presente().getPreco()))
                .limit(LIMITE_SUGESTOES)
                .toList();
    }

    /**
     * Presentes que o usuário cadastrou para a pessoa. Aparecem sempre, mesmo fora do orçamento ou sem
     * afinidade, pois foram escolhidos para ela; a avaliação só serve para explicar os motivos.
     */
    public List<SugestaoPresente> presentesDaPessoa(int usuarioId, int pessoaId) {
        Pessoa pessoa = pessoaService.buscarPorId(usuarioId, pessoaId);
        return presenteService.listarDaPessoa(usuarioId, pessoaId).stream()
                .map(presente -> avaliar(pessoa, presente))
                .toList();
    }

    /**
     * Pede ideias à IA e aplica as mesmas regras de orçamento e rejeição do catálogo,
     * pois o modelo nem sempre respeita as restrições pedidas. Cada ideia aprovada é procurada na loja
     * para ganhar o link da página do produto. Uma geração concluída com ao menos uma ideia vai para o
     * histórico; se a IA falhar, nada é registrado.
     * Cada conta pode fazer um número limitado de pedidos por hora; pedidos que falham também contam,
     * pois chegam à IA do mesmo jeito.
     * Ideias já sugeridas antes para a pessoa (pelo histórico) não se repetem: a IA recebe a lista delas
     * e, como nem sempre a respeita, as repetidas são descartadas pelo nome e pelo produto encontrado.
     */
    public ResultadoIdeias gerarIdeiasComIa(int usuarioId, int pessoaId) {
        Pessoa pessoa = pessoaService.buscarPorId(usuarioId, pessoaId);
        String chave = "ia:" + usuarioId;
        if (!limiteIa.tentarRegistrar(chave)) {
            throw new LimiteExcedidoException("Você atingiu o limite de " + limiteIa.getMaximo()
                    + " pedidos de ideias com IA por hora. Tente novamente em "
                    + LimiteDeTaxa.emMinutos(limiteIa.tempoParaLiberar(chave)) + ".", limiteIa.tempoParaLiberar(chave));
        }
        IdeiasAnteriores anteriores = ideiasAnteriores(usuarioId, pessoaId);
        List<IdeiaPresente> recebidas = geradorIdeias.gerarIdeias(pessoa, QUANTIDADE_IDEIAS_IA, anteriores.nomes());
        List<IdeiaPresente> aprovadas = recebidas.stream()
                .filter(ideia -> dentroDoOrcamento(ideia.precoEstimado(), pessoa.getOrcamento()))
                .filter(ideia -> !rejeitado(pessoa, List.of(ideia.nome(), ideia.categoria())))
                .toList();
        List<IdeiaPresente> ineditas = semRepeticoes(aprovadas, anteriores);
        List<IdeiaPresente> naLoja = semRepeticoes(procurarNaLoja(pessoa, ineditas), anteriores);
        if (!naLoja.isEmpty()) {
            historicoService.registrar(pessoa, OrigemSugestao.IA, naLoja.stream().map(ItemSugerido::de).toList());
        }
        return new ResultadoIdeias(naLoja, recebidas.size() - aprovadas.size(), aprovadas.size() - naLoja.size());
    }

    /**
     * Ideias já sugeridas para a pessoa: os nomes como foram exibidos (os mais recentes primeiro, para a IA)
     * e, para comparar, os nomes normalizados e os links de compra.
     */
    private record IdeiasAnteriores(List<String> nomes, Set<String> nomesNormalizados, Set<String> links) {
    }

    private IdeiasAnteriores ideiasAnteriores(int usuarioId, int pessoaId) {
        Set<String> nomes = new LinkedHashSet<>();
        Set<String> normalizados = new HashSet<>();
        Set<String> links = new HashSet<>();
        historicoService.listar(usuarioId, pessoaId).stream()
                .flatMap(registro -> registro.getItens().stream())
                .forEach(item -> {
                    if (normalizados.add(normalizarNome(item.nome()))) {
                        nomes.add(item.nome());
                    }
                    if (item.linkCompra() != null) {
                        links.add(item.linkCompra());
                    }
                });
        return new IdeiasAnteriores(List.copyOf(nomes), normalizados, links);
    }

    /** Remove as ideias já sugeridas antes e as repetidas dentro da própria lista (mesmo nome ou produto). */
    private List<IdeiaPresente> semRepeticoes(List<IdeiaPresente> ideias, IdeiasAnteriores anteriores) {
        Set<String> nomesVistos = new HashSet<>(anteriores.nomesNormalizados());
        Set<String> linksVistos = new HashSet<>(anteriores.links());
        List<IdeiaPresente> ineditas = new ArrayList<>();
        for (IdeiaPresente ideia : ideias) {
            boolean nomeNovo = nomesVistos.add(normalizarNome(ideia.nome()));
            boolean linkNovo = ideia.linkCompra() == null || linksVistos.add(ideia.linkCompra());
            if (nomeNovo && linkNovo) {
                ineditas.add(ideia);
            }
        }
        return ineditas;
    }

    /** "Caneca  de Cerâmica!" e "caneca de ceramica" contam como a mesma ideia. */
    private static String normalizarNome(String nome) {
        String semAcentos = Normalizer.normalize(nome == null ? "" : nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
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
        boolean ocasiaoCompativel = presente.getOcasioes().contains(pessoa.getOcasiao());

        int pontuacao = gostos.size() * PONTOS_POR_GOSTO;
        List<String> motivos = new ArrayList<>();
        if (!gostos.isEmpty()) {
            motivos.add("combina com os gostos: " + String.join(", ", gostos));
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
