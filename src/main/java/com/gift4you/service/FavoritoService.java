package com.gift4you.service;

import com.gift4you.exception.FavoritoNaoEncontradoException;
import com.gift4you.model.DadosFavorito;
import com.gift4you.model.Favorito;
import com.gift4you.model.ItemSugerido;
import com.gift4you.model.OrigemSugestao;
import com.gift4you.model.Presente;
import com.gift4you.repository.FavoritoRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class FavoritoService implements OuvinteRemocaoPessoa {

    private static final Comparator<Favorito> MAIS_RECENTES_PRIMEIRO =
            Comparator.comparing(Favorito::getFavoritadoEm).thenComparing(Favorito::getId).reversed();

    private final FavoritoRepository repository;
    private final FavoritoValidador validador;
    private final PessoaService pessoaService;
    private final PresenteService presenteService;
    private final LinkBuscaProduto linkBusca;
    private final Clock relogio;

    public FavoritoService(FavoritoRepository repository, FavoritoValidador validador, PessoaService pessoaService,
                           PresenteService presenteService, LinkBuscaProduto linkBusca, Clock relogio) {
        this.repository = repository;
        this.validador = validador;
        this.pessoaService = pessoaService;
        this.presenteService = presenteService;
        this.linkBusca = linkBusca;
        this.relogio = relogio;
    }

    /**
     * Favorita o item para a pessoa. Se já estiver nos favoritos, devolve o favorito existente.
     */
    public synchronized Favorito favoritar(DadosFavorito dados) {
        validador.validarPedido(dados);
        int pessoaId = pessoaService.buscarPorId(dados.pessoaId()).getId();
        ItemSugerido item = dados.origem() == OrigemSugestao.CATALOGO
                ? itemDoCatalogo(dados.item())
                : ideiaComLinkDeBusca(validador.validarIdeia(dados.item()));

        return repository.listarPorPessoa(pessoaId).stream()
                .filter(favorito -> favorito.refereSeA(dados.origem(), item))
                .findFirst()
                .orElseGet(() -> repository.salvar(
                        new Favorito(pessoaId, dados.origem(), item, LocalDateTime.now(relogio))));
    }

    /**
     * @param pessoaId filtra pela pessoa; se nulo, lista os favoritos de todas
     */
    public List<Favorito> listar(Integer pessoaId) {
        List<Favorito> favoritos = pessoaId == null
                ? repository.listarTodos()
                : repository.listarPorPessoa(pessoaId);
        return favoritos.stream().sorted(MAIS_RECENTES_PRIMEIRO).toList();
    }

    public void remover(int id) {
        if (!repository.remover(id)) {
            throw new FavoritoNaoEncontradoException(id);
        }
    }

    @Override
    public void aoRemoverPessoa(int pessoaId) {
        repository.removerPorPessoa(pessoaId);
    }

    /** O link é montado aqui, e não aceito do navegador, para não guardar links de terceiros. */
    private ItemSugerido ideiaComLinkDeBusca(ItemSugerido ideia) {
        return ideia.comLinkCompra(linkBusca.linkPara(ideia.nome()));
    }

    /** Usa os dados atuais do catálogo, e não os enviados, mantendo só os motivos da sugestão. */
    private ItemSugerido itemDoCatalogo(ItemSugerido enviado) {
        Presente presente = presenteService.buscarPorId(validador.exigirPresenteId(enviado));
        return ItemSugerido.de(presente, enviado.motivos() == null ? List.of() : enviado.motivos());
    }
}
