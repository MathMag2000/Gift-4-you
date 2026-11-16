package com.gift4you.repository;

import com.gift4you.model.DadosPessoa;
import com.gift4you.model.FaixaOrcamento;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.Pessoa;
import com.gift4you.model.Vinculo;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Optional;

import static com.gift4you.repository.ColunasSql.lerEnum;
import static com.gift4you.repository.ColunasSql.lerTextos;
import static com.gift4you.repository.ColunasSql.textos;

public class PessoaRepositoryJdbc implements PessoaRepository {

    private static final String COLUNAS = """
            SELECT id, usuario_id, nome, idade, vinculo, gostos, nao_gosta, ocasiao, orcamento_maximo
            FROM pessoa""";

    private static final RowMapper<Pessoa> MAPEADOR = (rs, linha) -> {
        DadosPessoa dados = new DadosPessoa(
                rs.getString("nome"),
                rs.getInt("idade"),
                lerEnum(rs, "vinculo", Vinculo.class),
                lerTextos(rs, "gostos"),
                lerTextos(rs, "nao_gosta"),
                lerEnum(rs, "ocasiao", Ocasiao.class),
                new FaixaOrcamento(rs.getBigDecimal("orcamento_maximo")));
        Pessoa pessoa = new Pessoa(rs.getInt("usuario_id"), dados);
        pessoa.setId(rs.getInt("id"));
        return pessoa;
    };

    private final JdbcClient jdbc;

    public PessoaRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pessoa salvar(Pessoa pessoa) {
        pessoa.setId(jdbc.sql("""
                        INSERT INTO pessoa (usuario_id, nome, idade, vinculo, gostos, nao_gosta, ocasiao, orcamento_maximo)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        RETURNING id""")
                .params(pessoa.getUsuarioId(), pessoa.getNome(), pessoa.getIdade(), pessoa.getVinculo().name(),
                        textos(pessoa.getGostos()), textos(pessoa.getNaoGosta()), pessoa.getOcasiao().name(),
                        pessoa.getOrcamento().maximo())
                .query(Integer.class)
                .single());
        return pessoa;
    }

    /** O dono da pessoa não muda: somente os dados do perfil são atualizados. */
    @Override
    public Pessoa atualizar(Pessoa pessoa) {
        jdbc.sql("""
                        UPDATE pessoa SET nome = ?, idade = ?, vinculo = ?, gostos = ?, nao_gosta = ?, ocasiao = ?,
                                          orcamento_maximo = ?
                        WHERE id = ?""")
                .params(pessoa.getNome(), pessoa.getIdade(), pessoa.getVinculo().name(), textos(pessoa.getGostos()),
                        textos(pessoa.getNaoGosta()), pessoa.getOcasiao().name(), pessoa.getOrcamento().maximo(),
                        pessoa.getId())
                .update();
        return pessoa;
    }

    @Override
    public Optional<Pessoa> buscarPorId(int id) {
        return jdbc.sql(COLUNAS + " WHERE id = ?").param(id).query(MAPEADOR).optional();
    }

    @Override
    public List<Pessoa> listarTodos() {
        return jdbc.sql(COLUNAS + " ORDER BY id").query(MAPEADOR).list();
    }

    @Override
    public List<Pessoa> listarPorUsuario(int usuarioId) {
        return jdbc.sql(COLUNAS + " WHERE usuario_id = ? ORDER BY id").param(usuarioId).query(MAPEADOR).list();
    }

    /** Favoritos e histórico da pessoa são apagados junto, pelo ON DELETE CASCADE do banco. */
    @Override
    public boolean remover(int id) {
        return jdbc.sql("DELETE FROM pessoa WHERE id = ?").param(id).update() > 0;
    }
}
