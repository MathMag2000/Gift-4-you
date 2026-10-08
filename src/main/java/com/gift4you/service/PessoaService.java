package com.gift4you.service;

import com.gift4you.exception.PessoaNaoEncontradaException;
import com.gift4you.model.DadosPessoa;
import com.gift4you.model.Pessoa;
import com.gift4you.repository.PessoaRepository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PessoaService {

    private final PessoaRepository repository;
    private final PessoaValidador validador;
    private final List<OuvinteRemocaoPessoa> ouvintesRemocao = new CopyOnWriteArrayList<>();

    public PessoaService(PessoaRepository repository, PessoaValidador validador) {
        this.repository = repository;
        this.validador = validador;
    }

    public Pessoa cadastrar(DadosPessoa dados) {
        return repository.salvar(new Pessoa(validador.validar(dados)));
    }

    public List<Pessoa> listar() {
        return repository.listarTodos();
    }

    public Pessoa buscarPorId(int id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new PessoaNaoEncontradaException(id));
    }

    public Pessoa alterar(int id, DadosPessoa dados) {
        Pessoa pessoa = buscarPorId(id);
        pessoa.atualizar(validador.validar(dados));
        return repository.atualizar(pessoa);
    }

    public void remover(int id) {
        if (!repository.remover(id)) {
            throw new PessoaNaoEncontradaException(id);
        }
        ouvintesRemocao.forEach(ouvinte -> ouvinte.aoRemoverPessoa(id));
    }

    public void adicionarOuvinteRemocao(OuvinteRemocaoPessoa ouvinte) {
        ouvintesRemocao.add(ouvinte);
    }
}
