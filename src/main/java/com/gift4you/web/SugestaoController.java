package com.gift4you.web;

import com.gift4you.model.ResultadoIdeias;
import com.gift4you.model.SugestaoPresente;
import com.gift4you.service.SugestaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pessoas/{pessoaId}")
public class SugestaoController {

    private final SugestaoService sugestaoService;

    public SugestaoController(SugestaoService sugestaoService) {
        this.sugestaoService = sugestaoService;
    }

    @GetMapping("/sugestoes")
    public List<SugestaoPresente> sugestoesDoCatalogo(@PathVariable int pessoaId) {
        return sugestaoService.gerarSugestoes(pessoaId);
    }

    /** POST porque cada chamada consulta a IA e pode gerar ideias diferentes. */
    @PostMapping("/ideias-ia")
    public ResultadoIdeias ideiasComIa(@PathVariable int pessoaId) {
        return sugestaoService.gerarIdeiasComIa(pessoaId);
    }
}
