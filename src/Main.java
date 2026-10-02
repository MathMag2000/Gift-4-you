import repository.PessoaRepositoryEmMemoria;
import repository.PresenteRepositoryEmMemoria;
import service.PessoaService;
import service.PessoaValidador;
import service.PresenteService;
import service.PresenteValidador;
import ui.LeitorConsole;
import ui.MenuPessoas;
import ui.MenuPresentes;
import ui.MenuPrincipal;

void main() {
    LeitorConsole leitor = new LeitorConsole();
    PessoaService pessoaService = new PessoaService(new PessoaRepositoryEmMemoria(), new PessoaValidador());
    PresenteService presenteService = new PresenteService(new PresenteRepositoryEmMemoria(), new PresenteValidador());

    new MenuPrincipal(leitor,
            new MenuPessoas(pessoaService, leitor),
            new MenuPresentes(presenteService, leitor)).exibir();
}
