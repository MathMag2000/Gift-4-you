package com.gift4you.service;

import com.gift4you.exception.PessoaNaoEncontradaException;
import com.gift4you.model.DadosPessoa;
import com.gift4you.model.Pessoa;
import com.gift4you.repository.PessoaRepository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cada pessoa pertence ao usuário que a cadastrou. Pessoas de outros usuários são tratadas como
 * inexistentes, para não revelar nem permitir alterar dados de outras contas.
 */
public class PessoaService {

    private final PessoaRepository repository;
    private final PessoaValidador validador;
    private final List<OuvinteRemocaoPessoa> ouvintesRemocao = new CopyOnWriteArrayList<>();

    public PessoaService(PessoaRepository repository, PessoaValidador validador) {
        this.repository = repository;
        this.validador = validador;
    }

    public Pessoa cadastrar(int usuarioId, DadosPessoa dados) {
        return repository.salvar(new Pessoa(usuarioId, validador.validar(dados)));
    }

    public List<Pessoa> listar(int usuarioId) {
        return repository.listarPorUsuario(usuarioId);
    }

    public List<Integer> idsDasPessoas(int usuarioId) {
        return listar(usuarioId).stream().map(Pessoa::getId).toList();
    }

    public Pessoa buscarPorId(int usuarioId, int id) {
        return repository.buscarPorId(id)
                .filter(pessoa -> pessoa.pertenceA(usuarioId))
                .orElseThrow(() -> new PessoaNaoEncontradaException(id));
    }

    public boolean pertenceAoUsuario(int usuarioId, int pessoaId) {
        return repository.buscarPorId(pessoaId).filter(pessoa -> pessoa.pertenceA(usuarioId)).isPresent();
    }

    public Pessoa alterar(int usuarioId, int id, DadosPessoa dados) {
        Pessoa pessoa = buscarPorId(usuarioId, id);
        pessoa.atualizar(validador.validar(dados));
        return repository.atualizar(pessoa);
    }

    public void remover(int usuarioId, int id) {
        buscarPorId(usuarioId, id);
        if (!repository.remover(id)) {
            throw new PessoaNaoEncontradaException(id);
        }
        ouvintesRemocao.forEach(ouvinte -> ouvinte.aoRemoverPessoa(id));
    }

    public void adicionarOuvinteRemocao(OuvinteRemocaoPessoa ouvinte) {
        ouvintesRemocao.add(ouvinte);
    }
}
