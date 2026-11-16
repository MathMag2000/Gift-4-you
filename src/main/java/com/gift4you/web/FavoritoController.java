package com.gift4you.web;

import com.gift4you.model.DadosFavorito;
import com.gift4you.model.Favorito;
import com.gift4you.service.FavoritoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @GetMapping
    public List<Favorito> listar(UsuarioLogado usuario, @RequestParam(required = false) Integer pessoaId) {
        return favoritoService.listar(usuario.id(), pessoaId);
    }

    /** Favoritar o mesmo item de novo devolve o favorito existente. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Favorito favoritar(UsuarioLogado usuario, @RequestBody DadosFavorito dados) {
        return favoritoService.favoritar(usuario.id(), dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(UsuarioLogado usuario, @PathVariable int id) {
        favoritoService.remover(usuario.id(), id);
    }
}
