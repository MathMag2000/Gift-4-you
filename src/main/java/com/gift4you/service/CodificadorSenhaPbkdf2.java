package com.gift4you.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Hash de senha com PBKDF2-HMAC-SHA256, disponível no próprio Java.
 * Cada senha recebe um sal aleatório, e o hash é guardado no formato
 * {@code pbkdf2_sha256$iteracoes$sal$hash}, para que o número de iterações possa
 * aumentar no futuro sem invalidar as senhas já cadastradas.
 */
public class CodificadorSenhaPbkdf2 implements CodificadorSenha {

    /** Recomendação da OWASP para PBKDF2-HMAC-SHA256. */
    public static final int ITERACOES_PADRAO = 600_000;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIXO = "pbkdf2_sha256";
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;

    private final int iteracoes;
    private final SecureRandom aleatorio = new SecureRandom();

    public CodificadorSenhaPbkdf2() {
        this(ITERACOES_PADRAO);
    }

    /** @param iteracoes valores menores deixam os testes mais rápidos; em produção use o padrão */
    public CodificadorSenhaPbkdf2(int iteracoes) {
        this.iteracoes = iteracoes;
    }

    @Override
    public String codificar(String senha) {
        byte[] sal = new byte[BYTES_SAL];
        aleatorio.nextBytes(sal);
        byte[] hash = derivar(senha, sal, iteracoes);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIXO + "$" + iteracoes + "$" + base64.encodeToString(sal) + "$" + base64.encodeToString(hash);
    }

    @Override
    public boolean confere(String senha, String hashGuardado) {
        if (senha == null || hashGuardado == null) {
            return false;
        }
        String[] partes = hashGuardado.split("\\$");
        if (partes.length != 4 || !partes[0].equals(PREFIXO)) {
            return false;
        }
        try {
            int iteracoesGuardadas = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            // Comparação em tempo constante, para não revelar quantos bytes coincidem.
            return MessageDigest.isEqual(esperado, derivar(senha, sal, iteracoesGuardadas));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derivar(String senha, byte[] sal, int numeroIteracoes) {
        PBEKeySpec especificacao = new PBEKeySpec(senha.toCharArray(), sal, numeroIteracoes, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacao).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algoritmo " + ALGORITMO + " indisponível no Java.", e);
        } finally {
            especificacao.clearPassword();
        }
    }
}
