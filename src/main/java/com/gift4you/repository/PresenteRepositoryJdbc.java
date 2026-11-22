package com.gift4you.repository;

import com.gift4you.model.Categoria;
import com.gift4you.model.DadosPresente;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.Presente;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Optional;

import static com.gift4you.repository.ColunasSql.lerEnum;
import static com.gift4you.repository.ColunasSql.lerEnums;
import static com.gift4you.repository.ColunasSql.lerInteiro;
import static com.gift4you.repository.ColunasSql.lerTextos;
import static com.gift4you.repository.ColunasSql.nomes;
import static com.gift4you.repository.ColunasSql.textos;

public class PresenteRepositoryJdbc implements PresenteRepository {

    private static final String COLUNAS = """
            SELECT id, usuario_id, nome, categoria, descricao, preco, caracteristicas, ocasioes, link_compra, imagem_url,
                   pessoa_id
            FROM presente""";

    private static final RowMapper<Presente> MAPEADOR = (rs, linha) -> {
        DadosPresente dados = new DadosPresente(
                rs.getString("nome"),
                lerEnum(rs, "categoria", Categoria.class),
                rs.getString("descricao"),
                rs.getBigDecimal("preco"),
                lerTextos(rs, "caracteristicas"),
                lerEnums(rs, "ocasioes", Ocasiao.class),
                rs.getString("link_compra"),
                lerInteiro(rs, "pessoa_id"));
        Presente presente = new Presente(rs.getInt("usuario_id"), dados);
        presente.setId(rs.getInt("id"));
        presente.definirImagem(rs.getString("imagem_url"));
        return presente;
    };

    private final JdbcClient jdbc;

    public PresenteRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Presente salvar(Presente presente) {
        presente.setId(jdbc.sql("""
                        INSERT INTO presente (usuario_id, nome, categoria, descricao, preco, caracteristicas, ocasioes,
                                              link_compra, imagem_url, pessoa_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        RETURNING id""")
                .params(presente.getUsuarioId(), presente.getNome(), presente.getCategoria().name(),
                        presente.getDescricao(), presente.getPreco(), textos(presente.getCaracteristicas()),
                        nomes(presente.getOcasioes()), presente.getLinkCompra(), presente.getImagemUrl(),
                        presente.getPessoaId())
                .query(Integer.class)
                .single());
        return presente;
    }

    @Override
    public Presente atualizar(Presente presente) {
        jdbc.sql("""
                        UPDATE presente SET nome = ?, categoria = ?, descricao = ?, preco = ?, caracteristicas = ?,
                                            ocasioes = ?, link_compra = ?, imagem_url = ?, pessoa_id = ?
                        WHERE id = ?""")
                .params(presente.getNome(), presente.getCategoria().name(), presente.getDescricao(), presente.getPreco(),
                        textos(presente.getCaracteristicas()), nomes(presente.getOcasioes()), presente.getLinkCompra(),
                        presente.getImagemUrl(), presente.getPessoaId(), presente.getId())
                .update();
        return presente;
    }

    @Override
    public Optional<Presente> buscarPorId(int id) {
        return jdbc.sql(COLUNAS + " WHERE id = ?").param(id).query(MAPEADOR).optional();
    }

    @Override
    public List<Presente> listarTodos() {
        return jdbc.sql(COLUNAS + " ORDER BY id").query(MAPEADOR).list();
    }

    @Override
    public List<Presente> listarPorUsuario(int usuarioId) {
        return jdbc.sql(COLUNAS + " WHERE usuario_id = ? ORDER BY id").param(usuarioId).query(MAPEADOR).list();
    }

    @Override
    public boolean remover(int id) {
        return jdbc.sql("DELETE FROM presente WHERE id = ?").param(id).update() > 0;
    }

    /** O banco já faz isso pelo ON DELETE SET NULL; fica explícito para não depender só do esquema. */
    @Override
    public void desvincularPessoa(int pessoaId) {
        jdbc.sql("UPDATE presente SET pessoa_id = NULL WHERE pessoa_id = ?").param(pessoaId).update();
    }
}
