package com.gift4you.web;

import com.gift4you.model.DadosPresente;
import com.gift4you.model.Presente;
import com.gift4you.service.PresenteService;
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

/** Catálogo de presentes da conta logada. */
@RestController
@RequestMapping("/api/presentes")
public class PresenteController {

    private final PresenteService presenteService;

    public PresenteController(PresenteService presenteService) {
        this.presenteService = presenteService;
    }

    @GetMapping
    public List<Presente> listar(UsuarioLogado usuario) {
        return presenteService.listar(usuario.id());
    }

    @GetMapping("/{id}")
    public Presente buscar(UsuarioLogado usuario, @PathVariable int id) {
        return presenteService.buscarPorId(usuario.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Presente cadastrar(UsuarioLogado usuario, @RequestBody DadosPresente dados) {
        return presenteService.cadastrar(usuario.id(), dados);
    }

    @PutMapping("/{id}")
    public Presente alterar(UsuarioLogado usuario, @PathVariable int id, @RequestBody DadosPresente dados) {
        return presenteService.alterar(usuario.id(), id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(UsuarioLogado usuario, @PathVariable int id) {
        presenteService.remover(usuario.id(), id);
    }
}
