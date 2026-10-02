package ui;

import exception.RegistroNaoEncontradoException;
import exception.ValidacaoException;

import java.util.List;

public abstract class Menu {

    private static final int SAIR = 0;

    protected final LeitorConsole leitor;

    protected Menu(LeitorConsole leitor) {
        this.leitor = leitor;
    }

    public void exibir() {
        int opcao;
        do {
            imprimirOpcoes();
            opcao = leitor.lerInteiro("Opção");
            executar(opcao);
        } while (opcao != SAIR);
    }

    protected abstract String titulo();

    protected abstract List<String> opcoes();

    /** Executa a opção escolhida, numerada a partir de 1 na ordem de {@link #opcoes()}. */
    protected abstract void executarOpcao(int opcao);

    protected String textoSair() {
        return "Voltar";
    }

    private void imprimirOpcoes() {
        IO.println("\n=== " + titulo() + " ===");
        List<String> opcoes = opcoes();
        for (int i = 0; i < opcoes.size(); i++) {
            IO.println((i + 1) + " - " + opcoes.get(i));
        }
        IO.println(SAIR + " - " + textoSair());
    }

    private void executar(int opcao) {
        if (opcao == SAIR) {
            return;
        }
        if (opcao < 1 || opcao > opcoes().size()) {
            IO.println("Opção inválida.");
            return;
        }
        try {
            executarOpcao(opcao);
        } catch (ValidacaoException | RegistroNaoEncontradoException e) {
            IO.println("Erro: " + e.getMessage());
        }
    }
}
