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

@RestController
@RequestMapping("/api/presentes")
public class PresenteController {

    private final PresenteService presenteService;

    public PresenteController(PresenteService presenteService) {
        this.presenteService = presenteService;
    }

    @GetMapping
    public List<Presente> listar() {
        return presenteService.listar();
    }

    @GetMapping("/{id}")
    public Presente buscar(@PathVariable int id) {
        return presenteService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Presente cadastrar(@RequestBody DadosPresente dados) {
        return presenteService.cadastrar(dados);
    }

    @PutMapping("/{id}")
    public Presente alterar(@PathVariable int id, @RequestBody DadosPresente dados) {
        return presenteService.alterar(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable int id) {
        presenteService.remover(id);
    }
}
