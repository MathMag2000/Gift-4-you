package ui;

import java.util.List;

public class MenuPrincipal extends Menu {

    private final MenuPessoas menuPessoas;
    private final MenuPresentes menuPresentes;

    public MenuPrincipal(LeitorConsole leitor, MenuPessoas menuPessoas, MenuPresentes menuPresentes) {
        super(leitor);
        this.menuPessoas = menuPessoas;
        this.menuPresentes = menuPresentes;
    }

    @Override
    protected String titulo() {
        return "Gift 4 You";
    }

    @Override
    protected List<String> opcoes() {
        return List.of("Pessoas", "Presentes");
    }

    @Override
    protected void executarOpcao(int opcao) {
        switch (opcao) {
            case 1 -> menuPessoas.exibir();
            case 2 -> menuPresentes.exibir();
        }
    }

    @Override
    protected String textoSair() {
        return "Sair";
    }
}
