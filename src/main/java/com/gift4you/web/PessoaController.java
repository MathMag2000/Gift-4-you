package com.gift4you.web;

import com.gift4you.model.DadosPessoa;
import com.gift4you.model.Pessoa;
import com.gift4you.service.PessoaService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Pessoas do usuário logado. */
@RestController
@RequestMapping("/api/pessoas")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    @GetMapping
    public List<Pessoa> listar(UsuarioLogado usuario) {
        return pessoaService.listar(usuario.id());
    }

    @GetMapping("/{id}")
    public Pessoa buscar(UsuarioLogado usuario, @PathVariable int id) {
        return pessoaService.buscarPorId(usuario.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Pessoa cadastrar(UsuarioLogado usuario, @RequestBody DadosPessoa dados) {
        return pessoaService.cadastrar(usuario.id(), dados);
    }

    @PutMapping("/{id}")
    public Pessoa alterar(UsuarioLogado usuario, @PathVariable int id, @RequestBody DadosPessoa dados) {
        return pessoaService.alterar(usuario.id(), id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(UsuarioLogado usuario, @PathVariable int id) {
        pessoaService.remover(usuario.id(), id);
    }
}
