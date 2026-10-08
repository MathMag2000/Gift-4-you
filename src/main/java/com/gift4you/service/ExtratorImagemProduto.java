package com.gift4you.service;

import java.util.Optional;

/**
 * Descobre a imagem de um produto a partir da página onde ele é vendido.
 */
public interface ExtratorImagemProduto {

    /**
     * @return endereço da imagem, ou vazio se a página não puder ser lida ou não informar imagem
     */
    Optional<String> extrair(String linkCompra);
}
