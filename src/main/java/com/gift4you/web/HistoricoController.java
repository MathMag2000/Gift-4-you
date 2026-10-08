package com.gift4you.web;

import com.gift4you.model.RegistroHistorico;
import com.gift4you.service.HistoricoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/historico")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping
    public List<RegistroHistorico> listar(@RequestParam(required = false) Integer pessoaId) {
        return historicoService.listar(pessoaId);
    }

    @GetMapping("/{id}")
    public RegistroHistorico buscar(@PathVariable int id) {
        return historicoService.buscarPorId(id);
    }
}
