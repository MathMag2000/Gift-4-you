package com.gift4you.web;

import com.gift4you.model.Categoria;
import com.gift4you.model.EstilosPredefinidos;
import com.gift4you.model.GostosPredefinidos;
import com.gift4you.model.Ocasiao;
import com.gift4you.model.OrigemSugestao;
import com.gift4you.model.Vinculo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Lista os valores aceitos nos campos de escolha, para o site montar os formulários.
 */
@RestController
public class OpcoesController {

    public record Opcao(String valor, String descricao) {
    }

    public record Opcoes(List<Opcao> vinculos, List<Opcao> ocasioes, List<Opcao> categorias, List<Opcao> origens,
                         List<Opcao> gostos, List<Opcao> estilos) {
    }

    @GetMapping("/api/opcoes")
    public Opcoes listar() {
        return new Opcoes(paraOpcoes(Vinculo.values()), paraOpcoes(Ocasiao.values()), paraOpcoes(Categoria.values()),
                paraOpcoes(OrigemSugestao.values()), paraOpcoes(GostosPredefinidos.TAGS),
                paraOpcoes(EstilosPredefinidos.TAGS));
    }

    private List<Opcao> paraOpcoes(List<String> tags) {
        return tags.stream().map(tag -> new Opcao(tag, tag)).toList();
    }

    private List<Opcao> paraOpcoes(Enum<?>[] valores) {
        return Arrays.stream(valores).map(valor -> new Opcao(valor.name(), valor.toString())).toList();
    }
}
