package com.gift4you.repository;

import com.gift4you.exception.EmailJaCadastradoException;
import com.gift4you.model.Usuario;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Optional;

public class UsuarioRepositoryJdbc implements UsuarioRepository {

    private static final String COLUNAS = "SELECT id, nome, email, senha_hash FROM usuario";

    private static final RowMapper<Usuario> MAPEADOR = (rs, linha) -> {
        Usuario usuario = new Usuario(rs.getString("nome"), rs.getString("email"), rs.getString("senha_hash"));
        usuario.setId(rs.getInt("id"));
        return usuario;
    };

    private final JdbcClient jdbc;

    public UsuarioRepositoryJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** A restrição UNIQUE do e-mail garante que não haja contas repetidas mesmo em cadastros simultâneos. */
    @Override
    public Usuario salvar(Usuario usuario) {
        try {
            usuario.setId(jdbc.sql("INSERT INTO usuario (nome, email, senha_hash) VALUES (?, ?, ?) RETURNING id")
                    .params(usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash())
                    .query(Integer.class)
                    .single());
            return usuario;
        } catch (DuplicateKeyException e) {
            throw new EmailJaCadastradoException();
        }
    }

    @Override
    public Usuario atualizar(Usuario usuario) {
        jdbc.sql("UPDATE usuario SET nome = ?, email = ?, senha_hash = ? WHERE id = ?")
                .params(usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash(), usuario.getId())
                .update();
        return usuario;
    }

    @Override
    public Optional<Usuario> buscarPorId(int id) {
        return jdbc.sql(COLUNAS + " WHERE id = ?").param(id).query(MAPEADOR).optional();
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return jdbc.sql(COLUNAS + " WHERE email = ?").param(email).query(MAPEADOR).optional();
    }

    @Override
    public List<Usuario> listarTodos() {
        return jdbc.sql(COLUNAS + " ORDER BY id").query(MAPEADOR).list();
    }

    @Override
    public boolean remover(int id) {
        return jdbc.sql("DELETE FROM usuario WHERE id = ?").param(id).update() > 0;
    }
}
