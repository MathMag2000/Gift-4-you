package com.gift4you.service;

import com.gift4you.exception.RegistroHistoricoNaoEncontradoException;
import com.gift4you.model.ItemSugerido;
import com.gift4you.model.OrigemSugestao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.RegistroHistorico;
import com.gift4you.repository.HistoricoRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class HistoricoService implements OuvinteRemocaoPessoa {

    private static final Comparator<RegistroHistorico> MAIS_RECENTES_PRIMEIRO =
            Comparator.comparing(RegistroHistorico::getRealizadoEm).thenComparing(RegistroHistorico::getId).reversed();

    private final HistoricoRepository repository;
    private final Clock relogio;

    public HistoricoService(HistoricoRepository repository, Clock relogio) {
        this.repository = repository;
        this.relogio = relogio;
    }

    public RegistroHistorico registrar(Pessoa pessoa, OrigemSugestao origem, List<ItemSugerido> itens) {
        return repository.salvar(new RegistroHistorico(pessoa.getId(), origem, pessoa.getOcasiao(),
                pessoa.getOrcamento(), itens, LocalDateTime.now(relogio)));
    }

    /**
     * @param pessoaId filtra pela pessoa; se nulo, lista o histórico de todas
     */
    public List<RegistroHistorico> listar(Integer pessoaId) {
        List<RegistroHistorico> registros = pessoaId == null
                ? repository.listarTodos()
                : repository.listarPorPessoa(pessoaId);
        return registros.stream().sorted(MAIS_RECENTES_PRIMEIRO).toList();
    }

    public RegistroHistorico buscarPorId(int id) {
        return repository.buscarPorId(id).orElseThrow(() -> new RegistroHistoricoNaoEncontradoException(id));
    }

    @Override
    public void aoRemoverPessoa(int pessoaId) {
        repository.removerPorPessoa(pessoaId);
    }
}
