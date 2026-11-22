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

/**
 * Histórico das ideias geradas com IA. Cada registro pertence à conta e continua disponível depois que a
 * pessoa é removida, indicando que ela foi excluída. Registros antigos de sugestões do catálogo, de quando
 * elas também eram registradas, não são mais exibidos.
 */
public class HistoricoService implements OuvinteRemocaoPessoa {

    private static final Comparator<RegistroHistorico> MAIS_RECENTES_PRIMEIRO =
            Comparator.comparing(RegistroHistorico::getRealizadoEm).thenComparing(RegistroHistorico::getId).reversed();

    private final HistoricoRepository repository;
    private final PessoaService pessoaService;
    private final Clock relogio;

    public HistoricoService(HistoricoRepository repository, PessoaService pessoaService, Clock relogio) {
        this.repository = repository;
        this.pessoaService = pessoaService;
        this.relogio = relogio;
    }

    public RegistroHistorico registrar(Pessoa pessoa, OrigemSugestao origem, List<ItemSugerido> itens) {
        return repository.salvar(new RegistroHistorico(pessoa.getUsuarioId(), pessoa.getId(), pessoa.getNome(),
                origem, pessoa.getOcasiao(), pessoa.getOrcamento(), itens, LocalDateTime.now(relogio)));
    }

    /**
     * @param pessoaId filtra pela pessoa; se nulo, lista o histórico de toda a conta, inclusive o de pessoas
     *                 já removidas. Uma pessoa de outro usuário resulta numa lista vazia.
     */
    public List<RegistroHistorico> listar(int usuarioId, Integer pessoaId) {
        List<RegistroHistorico> registros;
        if (pessoaId == null) {
            registros = repository.listarPorUsuario(usuarioId);
        } else if (pessoaService.pertenceAoUsuario(usuarioId, pessoaId)) {
            registros = repository.listarPorPessoa(pessoaId);
        } else {
            registros = List.of();
        }
        return registros.stream()
                .filter(registro -> registro.pertenceA(usuarioId) && registro.getOrigem() == OrigemSugestao.IA)
                .sorted(MAIS_RECENTES_PRIMEIRO)
                .toList();
    }

    public RegistroHistorico buscarPorId(int usuarioId, int id) {
        return repository.buscarPorId(id)
                .filter(registro -> registro.pertenceA(usuarioId))
                .orElseThrow(() -> new RegistroHistoricoNaoEncontradoException(id));
    }

    /** A pessoa saiu, mas as ideias geradas para ela continuam no histórico. */
    @Override
    public void aoRemoverPessoa(int pessoaId) {
        repository.desvincularPessoa(pessoaId);
    }
}
