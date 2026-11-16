package com.gift4you.repository;

import com.gift4you.model.Favorito;
import com.gift4you.model.ItemSugerido;
import com.gift4you.model.OrigemSugestao;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.gift4you.repository.ColunasSql.ids;
import static com.gift4you.repository.ColunasSql.lerEnum;
import static com.gift4you.repository.ColunasSql.lerInteiro;
import static com.gift4you.repository.ColunasSql.lerTextos;
import static com.gift4you.repository.ColunasSql.textos;

public class FavoritoRepositoryJdbc implements FavoritoRepository {

    private static final String COLUNAS = """
            SELECT id, pessoa_id, origem, presente_id, nome, categoria, preco, motivos, link_compra, imagem_url,
                   favoritado_em
            FROM favorito""";

    private static final RowMapper<Favorito> MAPEADOR = (rs, linha) -> {
        ItemSugerido item = new ItemSugerido(lerInteiro(rs, "presente_id"), rs.getString("nome"),
                rs.getString("categoria"), rs.getBigDecimal("preco"), lerTextos(rs, "motivos"),
                rs.getString("link_compra"), rs.getString("imagem_url"));
        Favorito favorito = new Favorito(rs.getInt("pessoa_id"), lerEnum(rs, "origem", OrigemSugestao.class), item,
                rs.getObject("favoritado_em", LocalDateTime.class));
        favorito.setId(rs.getInt("id"));
        return favorito;
    };

    private final JdbcClient jdbc;

    public FavoritoRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Favorito salvar(Favorito favorito) {
        ItemSugerido item = favorito.getItem();
        favorito.setId(jdbc.sql("""
                        INSERT INTO favorito (pessoa_id, origem, presente_id, nome, categoria, preco, motivos, link_compra,
                                              imagem_url, favoritado_em)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        RETURNING id""")
                .params(favorito.getPessoaId(), favorito.getOrigem().name(), item.presenteId(), item.nome(),
                        item.categoria(), item.preco(), textos(item.motivos()), item.linkCompra(), item.imagemUrl(),
                        favorito.getFavoritadoEm())
                .query(Integer.class)
                .single());
        return favorito;
    }

    /** Favoritos não mudam depois de criados: são apenas adicionados ou removidos. */
    @Override
    public Favorito atualizar(Favorito favorito) {
        throw new UnsupportedOperationException("Favoritos não podem ser alterados.");
    }

    @Override
    public Optional<Favorito> buscarPorId(int id) {
        return jdbc.sql(COLUNAS + " WHERE id = ?").param(id).query(MAPEADOR).optional();
    }

    @Override
    public List<Favorito> listarTodos() {
        return jdbc.sql(COLUNAS + " ORDER BY id").query(MAPEADOR).list();
    }

    @Override
    public List<Favorito> listarPorPessoa(int pessoaId) {
        return jdbc.sql(COLUNAS + " WHERE pessoa_id = ? ORDER BY id").param(pessoaId).query(MAPEADOR).list();
    }

    @Override
    public List<Favorito> listarPorPessoas(Collection<Integer> pessoaIds) {
        if (pessoaIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql(COLUNAS + " WHERE pessoa_id = ANY (?) ORDER BY id").param(ids(pessoaIds))
                .query(MAPEADOR).list();
    }

    @Override
    public boolean remover(int id) {
        return jdbc.sql("DELETE FROM favorito WHERE id = ?").param(id).update() > 0;
    }

    @Override
    public void removerPorPessoa(int pessoaId) {
        jdbc.sql("DELETE FROM favorito WHERE pessoa_id = ?").param(pessoaId).update();
    }
}
