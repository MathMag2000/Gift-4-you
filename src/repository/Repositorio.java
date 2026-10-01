package repository;

import model.Identificavel;

import java.util.List;
import java.util.Optional;

public interface Repositorio<T extends Identificavel> {

    T salvar(T entidade);

    T atualizar(T entidade);

    Optional<T> buscarPorId(int id);

    List<T> listarTodos();

    boolean remover(int id);
}
