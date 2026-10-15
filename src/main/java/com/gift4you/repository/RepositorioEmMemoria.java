package com.gift4you.repository;

import com.gift4you.model.Identificavel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class RepositorioEmMemoria<T extends Identificavel> implements Repositorio<T> {

    private final Map<Integer, T> entidades = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public synchronized T salvar(T entidade) {
        entidade.setId(proximoId++);
        entidades.put(entidade.getId(), entidade);
        return entidade;
    }

    @Override
    public synchronized T atualizar(T entidade) {
        entidades.put(entidade.getId(), entidade);
        return entidade;
    }

    @Override
    public synchronized Optional<T> buscarPorId(int id) {
        return Optional.ofNullable(entidades.get(id));
    }

    @Override
    public synchronized List<T> listarTodos() {
        return new ArrayList<>(entidades.values());
    }

    @Override
    public synchronized boolean remover(int id) {
        return entidades.remove(id) != null;
    }
}
