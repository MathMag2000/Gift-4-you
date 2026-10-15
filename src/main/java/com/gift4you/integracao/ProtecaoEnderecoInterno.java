package com.gift4you.integracao;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Impede que o servidor acesse endereços da rede interna (localhost, IPs privados) a partir de
 * links informados por usuários, o que permitiria usar o site para alcançar serviços internos (SSRF).
 */
public class ProtecaoEnderecoInterno {

    public boolean permitido(URI endereco) {
        String esquema = endereco.getScheme();
        if (esquema == null || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))) {
            return false;
        }
        if (endereco.getHost() == null) {
            return false;
        }
        try {
            for (InetAddress ip : InetAddress.getAllByName(endereco.getHost())) {
                if (interno(ip)) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean interno(InetAddress ip) {
        byte[] bytes = ip.getAddress();
        boolean ipv6LocalUnico = bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
        boolean redeCompartilhada = bytes.length == 4 && (bytes[0] & 0xFF) == 100 && (bytes[1] & 0xC0) == 64;
        return ip.isLoopbackAddress() || ip.isAnyLocalAddress() || ip.isLinkLocalAddress()
                || ip.isSiteLocalAddress() || ip.isMulticastAddress() || ipv6LocalUnico || redeCompartilhada;
    }
}
