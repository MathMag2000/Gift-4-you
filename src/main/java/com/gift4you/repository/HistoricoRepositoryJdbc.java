package com.gift4you.repository;

import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.ItemSugerido;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.OrigemSugestao;
import com.gift4you.model.RegistroHistorico;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.gift4you.repository.ColunasSql.ids;
import static com.gift4you.repository.ColunasSql.lerEnum;
import static com.gift4you.repository.ColunasSql.lerInteiro;
import static com.gift4you.repository.ColunasSql.lerTextos;
import static com.gift4you.repository.ColunasSql.textos;

/**
 * Cada registro fica na tabela registro_historico e os itens sugeridos nele, em item_historico.
 * O registro pertence à conta (usuario_id); pessoa_id fica nulo quando a pessoa é removida.
 */
public class HistoricoRepositoryJdbc implements HistoricoRepository {

    private static final String COLUNAS = """
            SELECT id, usuario_id, pessoa_id, pessoa_nome, origem, ocasiao, orcamento_maximo, realizado_em
            FROM registro_historico""";

    /** Registro lido do banco, ainda sem os itens, que vêm de outra consulta. */
    private record LinhaRegistro(int id, int usuarioId, Integer pessoaId, String pessoaNome, OrigemSugestao origem,
                                 Ocasiao ocasiao, BigDecimal orcamentoMaximo, LocalDateTime realizadoEm) {
    }

    private record LinhaItem(int registroId, ItemSugerido item) {
    }

    private static final RowMapper<LinhaRegistro> MAPEADOR_REGISTRO = (rs, linha) -> new LinhaRegistro(
            rs.getInt("id"), rs.getInt("usuario_id"), lerInteiro(rs, "pessoa_id"), rs.getString("pessoa_nome"),
            lerEnum(rs, "origem", OrigemSugestao.class), lerEnum(rs, "ocasiao", Ocasiao.class),
            rs.getBigDecimal("orcamento_maximo"), rs.getObject("realizado_em", LocalDateTime.class));

    private static final RowMapper<LinhaItem> MAPEADOR_ITEM = (rs, linha) -> new LinhaItem(rs.getInt("registro_id"),
            new ItemSugerido(lerInteiro(rs, "presente_id"), rs.getString("nome"), rs.getString("categoria"),
                    rs.getBigDecimal("preco"), lerTextos(rs, "motivos"), rs.getString("link_compra"),
                    rs.getString("imagem_url")));

    private final JdbcClient jdbc;
    private final TransactionOperations transacao;

    public HistoricoRepositoryJdbc(JdbcClient jdbc, TransactionOperations transacao) {
        this.jdbc = jdbc;
        this.transacao = transacao;
    }

    /** Grava o registro e os itens juntos: se algo falhar, nada fica gravado pela metade. */
    @Override
    public RegistroHistorico salvar(RegistroHistorico registro) {
        return transacao.execute(status -> {
            int id = jdbc.sql("""
                            INSERT INTO registro_historico (usuario_id, pessoa_id, pessoa_nome, origem, ocasiao,
                                                            orcamento_maximo, realizado_em)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            RETURNING id""")
                    .params(registro.getUsuarioId(), registro.getPessoaId(), registro.getPessoaNome(),
                            registro.getOrigem().name(), registro.getOcasiao().name(),
                            registro.getOrcamento().maximo(), registro.getRealizadoEm())
                    .query(Integer.class)
                    .single();
            List<ItemSugerido> itens = registro.getItens();
            for (int posicao = 0; posicao < itens.size(); posicao++) {
                ItemSugerido item = itens.get(posicao);
                jdbc.sql("""
                                INSERT INTO item_historico (registro_id, posicao, presente_id, nome, categoria, preco,
                                                            motivos, link_compra, imagem_url)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""")
                        .params(id, posicao, item.presenteId(), item.nome(), item.categoria(), item.preco(),
                                textos(item.motivos()), item.linkCompra(), item.imagemUrl())
                        .update();
            }
            registro.setId(id);
            return registro;
        });
    }

    /** Registros do histórico não mudam depois de criados. */
    @Override
    public RegistroHistorico atualizar(RegistroHistorico registro) {
        throw new UnsupportedOperationException("Registros do histórico não podem ser alterados.");
    }

    @Override
    public Optional<RegistroHistorico> buscarPorId(int id) {
        return montar(jdbc.sql(COLUNAS + " WHERE id = ?").param(id).query(MAPEADOR_REGISTRO).list())
                .stream().findFirst();
    }

    @Override
    public List<RegistroHistorico> listarTodos() {
        return montar(jdbc.sql(COLUNAS + " ORDER BY id").query(MAPEADOR_REGISTRO).list());
    }

    @Override
    public List<RegistroHistorico> listarPorUsuario(int usuarioId) {
        return montar(jdbc.sql(COLUNAS + " WHERE usuario_id = ? ORDER BY id").param(usuarioId)
                .query(MAPEADOR_REGISTRO).list());
    }

    @Override
    public List<RegistroHistorico> listarPorPessoa(int pessoaId) {
        return montar(jdbc.sql(COLUNAS + " WHERE pessoa_id = ? ORDER BY id").param(pessoaId)
                .query(MAPEADOR_REGISTRO).list());
    }

    /** Os itens são apagados junto, pelo ON DELETE CASCADE do banco. */
    @Override
    public boolean remover(int id) {
        return jdbc.sql("DELETE FROM registro_historico WHERE id = ?").param(id).update() > 0;
    }

    /** O banco já faz isso pelo ON DELETE SET NULL; fica explícito para não depender só do esquema. */
    @Override
    public void desvincularPessoa(int pessoaId) {
        jdbc.sql("UPDATE registro_historico SET pessoa_id = NULL WHERE pessoa_id = ?").param(pessoaId).update();
    }

    /** Busca os itens de todos os registros numa só consulta, em vez de uma consulta por registro. */
    private List<RegistroHistorico> montar(List<LinhaRegistro> linhas) {
        if (linhas.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<ItemSugerido>> itensPorRegistro = new HashMap<>();
        jdbc.sql("""
                        SELECT registro_id, presente_id, nome, categoria, preco, motivos, link_compra, imagem_url
                        FROM item_historico
                        WHERE registro_id = ANY (?)
                        ORDER BY registro_id, posicao""")
                .param(ids(linhas.stream().map(LinhaRegistro::id).toList()))
                .query(MAPEADOR_ITEM)
                .list()
                .forEach(linha -> itensPorRegistro.computeIfAbsent(linha.registroId(), id -> new ArrayList<>())
                        .add(linha.item()));

        return linhas.stream().map(linha -> {
            RegistroHistorico registro = new RegistroHistorico(linha.usuarioId(), linha.pessoaId(), linha.pessoaNome(),
                    linha.origem(), linha.ocasiao(), new FaixaOrcamento(linha.orcamentoMaximo()),
                    itensPorRegistro.getOrDefault(linha.id(), List.of()), linha.realizadoEm());
            registro.setId(linha.id());
            return registro;
        }).toList();
    }
}
