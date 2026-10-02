package repository;

import model.Identificavel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class RepositorioEmMemoria<T extends Identificavel> implements Repositorio<T> {

    private final Map<Integer, T> entidades = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public T salvar(T entidade) {
        entidade.setId(proximoId++);
        entidades.put(entidade.getId(), entidade);
        return entidade;
    }

    @Override
    public T atualizar(T entidade) {
        entidades.put(entidade.getId(), entidade);
        return entidade;
    }

    @Override
    public Optional<T> buscarPorId(int id) {
        return Optional.ofNullable(entidades.get(id));
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(entidades.values());
    }

    @Override
    public boolean remover(int id) {
        return entidades.remove(id) != null;
    }
}
