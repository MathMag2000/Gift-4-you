package service;

import exception.PresenteNaoEncontradoException;
import model.DadosPresente;
import model.Presente;
import repository.PresenteRepository;

import java.util.List;

public class PresenteService {

    private final PresenteRepository repository;
    private final PresenteValidador validador;

    public PresenteService(PresenteRepository repository, PresenteValidador validador) {
        this.repository = repository;
        this.validador = validador;
    }

    public Presente cadastrar(DadosPresente dados) {
        return repository.salvar(new Presente(validador.validar(dados)));
    }

    public List<Presente> listar() {
        return repository.listarTodos();
    }

    public Presente buscarPorId(int id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new PresenteNaoEncontradoException(id));
    }

    public Presente alterar(int id, DadosPresente dados) {
        Presente presente = buscarPorId(id);
        presente.atualizar(validador.validar(dados));
        return repository.atualizar(presente);
    }

    public void remover(int id) {
        if (!repository.remover(id)) {
            throw new PresenteNaoEncontradoException(id);
        }
    }
}
