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
    private final LojaOnline loja;
    private final Clock relogio;

    public FavoritoService(FavoritoRepository repository, FavoritoValidador validador, PessoaService pessoaService,
                           PresenteService presenteService, LojaOnline loja, Clock relogio) {
        this.repository = repository;
        this.validador = validador;
        this.pessoaService = pessoaService;
        this.presenteService = presenteService;
        this.loja = loja;
        this.relogio = relogio;
    }

    /**
     * Favorita o item para a pessoa, que deve ser do usuário. Se já estiver nos favoritos, devolve o
     * favorito existente.
     */
    public synchronized Favorito favoritar(int usuarioId, DadosFavorito dados) {
        validador.validarPedido(dados);
        int pessoaId = pessoaService.buscarPorId(usuarioId, dados.pessoaId()).getId();
        ItemSugerido item = dados.origem() == OrigemSugestao.CATALOGO
                ? itemDoCatalogo(dados.item())
                : ideiaComLinkDaLoja(validador.validarIdeia(dados.item()), dados.item());

        return repository.listarPorPessoa(pessoaId).stream()
                .filter(favorito -> favorito.refereSeA(dados.origem(), item))
                .findFirst()
                .orElseGet(() -> repository.salvar(
                        new Favorito(pessoaId, dados.origem(), item, LocalDateTime.now(relogio))));
    }

    /**
     * @param pessoaId filtra pela pessoa; se nulo, lista os favoritos de todas as pessoas do usuário.
     *                 Uma pessoa de outro usuário resulta numa lista vazia.
     */
    public List<Favorito> listar(int usuarioId, Integer pessoaId) {
        List<Favorito> favoritos;
        if (pessoaId == null) {
            favoritos = repository.listarPorPessoas(pessoaService.idsDasPessoas(usuarioId));
        } else if (pessoaService.pertenceAoUsuario(usuarioId, pessoaId)) {
            favoritos = repository.listarPorPessoa(pessoaId);
        } else {
            favoritos = List.of();
        }
        return favoritos.stream().sorted(MAIS_RECENTES_PRIMEIRO).toList();
    }

    /** Favoritos de pessoas de outro usuário são tratados como inexistentes. */
    public void remover(int usuarioId, int id) {
        boolean doUsuario = repository.buscarPorId(id)
                .filter(favorito -> pessoaService.pertenceAoUsuario(usuarioId, favorito.getPessoaId()))
                .isPresent();
        if (!doUsuario || !repository.remover(id)) {
            throw new FavoritoNaoEncontradoException(id);
        }
    }

    @Override
    public void aoRemoverPessoa(int pessoaId) {
        repository.removerPorPessoa(pessoaId);
    }

    /**
     * Mantém o link e a foto enviados somente se forem de um produto da loja; caso contrário usa o link
     * de busca. Assim não se guarda link de outro site, como um endereço malicioso.
     */
    private ItemSugerido ideiaComLinkDaLoja(ItemSugerido ideia, ItemSugerido enviado) {
        String link = loja.ehLinkDeProduto(enviado.linkCompra()) ? enviado.linkCompra() : loja.linkBusca(ideia.nome());
        String imagem = loja.ehImagemDaLoja(enviado.imagemUrl()) ? enviado.imagemUrl() : null;
        return ideia.naLoja(link, imagem);
    }

    /** Usa os dados atuais do catálogo, e não os enviados, mantendo só os motivos da sugestão. */
    private ItemSugerido itemDoCatalogo(ItemSugerido enviado) {
        Presente presente = presenteService.buscarPorId(validador.exigirPresenteId(enviado));
        return ItemSugerido.de(presente, enviado.motivos() == null ? List.of() : enviado.motivos());
    }
}
