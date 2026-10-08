package com.gift4you.integracao;

import com.gift4you.service.LinkBuscaProduto;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class BuscaAmazon implements LinkBuscaProduto {

    private static final String ENDERECO_BUSCA = "https://www.amazon.com.br/s?k=";

    @Override
    public String linkPara(String nomeProduto) {
        return ENDERECO_BUSCA + URLEncoder.encode(nomeProduto.trim(), StandardCharsets.UTF_8);
    }
}
