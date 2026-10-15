package com.gift4you.service;

import com.gift4you.exception.PresenteNaoEncontradoException;
import com.gift4you.model.DadosPresente;
import com.gift4you.model.Presente;
import com.gift4you.repository.PresenteRepository;

import java.util.List;

public class PresenteService {

    private final PresenteRepository repository;
    private final PresenteValidador validador;
    private final ExtratorImagemProduto extratorImagem;

    public PresenteService(PresenteRepository repository, PresenteValidador validador,
                           ExtratorImagemProduto extratorImagem) {
        this.repository = repository;
        this.validador = validador;
        this.extratorImagem = extratorImagem;
    }

    /** Cadastra o presente buscando a imagem na página do link de compra. */
    public Presente cadastrar(DadosPresente dados) {
        Presente presente = new Presente(validador.validar(dados));
        presente.definirImagem(extratorImagem.extrair(presente.getLinkCompra()).orElse(null));
        return repository.salvar(presente);
    }

    public List<Presente> listar() {
        return repository.listarTodos();
    }

    public Presente buscarPorId(int id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new PresenteNaoEncontradoException(id));
    }

    /**
     * Busca a imagem de novo quando o link muda ou quando a tentativa anterior não encontrou imagem.
     */
    public Presente alterar(int id, DadosPresente dados) {
        Presente presente = buscarPorId(id);
        DadosPresente validados = validador.validar(dados);
        boolean buscarImagem = presente.getImagemUrl() == null
                || !presente.getLinkCompra().equals(validados.linkCompra());
        presente.atualizar(validados);
        if (buscarImagem) {
            presente.definirImagem(extratorImagem.extrair(presente.getLinkCompra()).orElse(null));
        }
        return repository.atualizar(presente);
    }

    public void remover(int id) {
        if (!repository.remover(id)) {
            throw new PresenteNaoEncontradoException(id);
        }
    }
}
