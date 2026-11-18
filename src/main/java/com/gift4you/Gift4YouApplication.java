package com.gift4you;

import com.gift4you.config.VerificacaoProducao;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Gift4YouApplication {

    public static void main(String[] args) {
        SpringApplication aplicacao = new SpringApplication(Gift4YouApplication.class);
        aplicacao.addListeners(new VerificacaoProducao());
        aplicacao.run(args);
    }
}
