package com.gift4you.web;

import com.gift4you.exception.IntegracaoIaException;
import com.gift4you.exception.RegistroNaoEncontradoException;
import com.gift4you.exception.ValidacaoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Converte as exceções do sistema em respostas HTTP com uma mensagem legível para o site.
 */
@RestControllerAdvice
public class TratadorErros {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorErros.class);

    public record Erro(String mensagem) {
    }

    @ExceptionHandler(ValidacaoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Erro validacao(ValidacaoException e) {
        return new Erro(e.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Erro dadosInvalidos(Exception e) {
        LOG.warn("Requisição com dados em formato inválido: {}", e.getMessage());
        return new Erro("Dados enviados em formato inválido.");
    }

    @ExceptionHandler(RegistroNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Erro naoEncontrado(RegistroNaoEncontradoException e) {
        return new Erro(e.getMessage());
    }

    @ExceptionHandler(IntegracaoIaException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Erro integracaoIa(IntegracaoIaException e) {
        return new Erro(e.getMessage());
    }
}
