package com.gift4you.repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Conversões entre os tipos do sistema e as colunas do PostgreSQL usadas pelos repositórios JDBC.
 * Listas de textos e de enums são guardadas em colunas {@code TEXT[]}.
 */
final class ColunasSql {

    private ColunasSql() {
    }

    static String[] textos(List<String> lista) {
        return lista == null ? new String[0] : lista.toArray(String[]::new);
    }

    static String[] nomes(List<? extends Enum<?>> valores) {
        return valores.stream().map(Enum::name).toArray(String[]::new);
    }

    static int[] ids(Collection<Integer> ids) {
        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    static List<String> lerTextos(ResultSet rs, String coluna) throws SQLException {
        Array array = rs.getArray(coluna);
        if (array == null) {
            return List.of();
        }
        try {
            return List.of((String[]) array.getArray());
        } finally {
            array.free();
        }
    }

    static <E extends Enum<E>> List<E> lerEnums(ResultSet rs, String coluna, Class<E> tipo) throws SQLException {
        return lerTextos(rs, coluna).stream().map(nome -> Enum.valueOf(tipo, nome)).toList();
    }

    static <E extends Enum<E>> E lerEnum(ResultSet rs, String coluna, Class<E> tipo) throws SQLException {
        return Enum.valueOf(tipo, rs.getString(coluna));
    }

    /** Lê uma coluna inteira que pode ser nula. */
    static Integer lerInteiro(ResultSet rs, String coluna) throws SQLException {
        int valor = rs.getInt(coluna);
        return rs.wasNull() ? null : valor;
    }
}
