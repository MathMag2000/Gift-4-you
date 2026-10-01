package ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lê dados do console. Nos métodos que recebem um valor atual, deixar a entrada
 * em branco mantém esse valor (usado na alteração); com atual nulo o campo é preenchido do zero.
 */
public class LeitorConsole {

    private static final String LIMPAR = "-";

    public String lerTexto(String mensagem) {
        String entrada = IO.readln(mensagem);
        if (entrada == null) {
            throw new IllegalStateException("Entrada encerrada.");
        }
        return entrada;
    }

    public String lerTexto(String mensagem, String atual) {
        String entrada = lerTexto(mensagem + sufixoAtual(atual));
        return entrada.isBlank() && atual != null ? atual : entrada;
    }

    public String lerTextoOpcional(String mensagem, String atual) {
        if (atual == null) {
            return lerTexto(mensagem + ": ");
        }
        String entrada = lerTexto(mensagem + " (\"-\" limpa)" + sufixoAtual(atual.isEmpty() ? "-" : atual));
        if (entrada.trim().equals(LIMPAR)) {
            return "";
        }
        return entrada.isBlank() ? atual : entrada;
    }

    public int lerInteiro(String mensagem) {
        return lerInteiro(mensagem, null);
    }

    public int lerInteiro(String mensagem, Integer atual) {
        while (true) {
            String entrada = lerTexto(mensagem + sufixoAtual(atual)).trim();
            if (entrada.isEmpty() && atual != null) {
                return atual;
            }
            try {
                return Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                IO.println("Digite um número inteiro válido.");
            }
        }
    }

    public BigDecimal lerValor(String mensagem, BigDecimal atual) {
        String valorAtual = atual == null ? null : atual.toPlainString().replace('.', ',');
        while (true) {
            String entrada = lerTexto(mensagem + sufixoAtual(valorAtual)).trim();
            if (entrada.isEmpty() && atual != null) {
                return atual;
            }
            try {
                return new BigDecimal(entrada.replace(",", "."));
            } catch (NumberFormatException e) {
                IO.println("Digite um valor válido (ex.: 150 ou 150,90).");
            }
        }
    }

    public List<String> lerLista(String mensagem, List<String> atual) {
        String dica = atual == null ? " (separados por vírgula)" : " (vírgula separa, \"-\" limpa)";
        String entrada = lerTexto(mensagem + dica + sufixoAtual(atual == null ? null : Formatador.lista(atual)));
        if (atual != null && entrada.isBlank()) {
            return atual;
        }
        if (entrada.trim().equals(LIMPAR)) {
            return List.of();
        }
        return Arrays.asList(entrada.split(","));
    }

    public <E extends Enum<E>> E lerOpcao(String titulo, E[] opcoes, E atual) {
        imprimirOpcoes(titulo, opcoes);
        while (true) {
            int escolha = lerInteiro("Escolha", atual == null ? null : atual.ordinal() + 1);
            if (escolha >= 1 && escolha <= opcoes.length) {
                return opcoes[escolha - 1];
            }
            IO.println("Escolha um número entre 1 e " + opcoes.length + ".");
        }
    }

    public <E extends Enum<E>> List<E> lerOpcoes(String titulo, E[] opcoes, List<E> atuais) {
        imprimirOpcoes(titulo, opcoes);
        String numerosAtuais = atuais == null ? null
                : atuais.stream().map(opcao -> String.valueOf(opcao.ordinal() + 1)).collect(Collectors.joining(","));
        while (true) {
            String entrada = lerTexto("Escolhas (separadas por vírgula)" + sufixoAtual(numerosAtuais));
            if (entrada.isBlank() && atuais != null) {
                return atuais;
            }
            try {
                List<E> escolhidas = new ArrayList<>();
                for (String numero : entrada.split(",")) {
                    if (numero.isBlank()) {
                        continue;
                    }
                    int escolha = Integer.parseInt(numero.trim());
                    if (escolha < 1 || escolha > opcoes.length) {
                        throw new NumberFormatException();
                    }
                    escolhidas.add(opcoes[escolha - 1]);
                }
                return escolhidas;
            } catch (NumberFormatException e) {
                IO.println("Use números entre 1 e " + opcoes.length + " separados por vírgula (ex.: 1,3).");
            }
        }
    }

    private void imprimirOpcoes(String titulo, Object[] opcoes) {
        IO.println(titulo + ":");
        for (int i = 0; i < opcoes.length; i++) {
            IO.println("  " + (i + 1) + " - " + opcoes[i]);
        }
    }

    private String sufixoAtual(Object atual) {
        return atual == null ? ": " : " [" + atual + "]: ";
    }
}
