package ui;

import model.DadosPessoa;
import model.FaixaOrcamento;
import model.Ocasiao;
import model.Pessoa;
import model.Vinculo;
import service.PessoaService;

import java.math.BigDecimal;
import java.util.List;

public class MenuPessoas extends Menu {

    private final PessoaService pessoaService;

    public MenuPessoas(PessoaService pessoaService, LeitorConsole leitor) {
        super(leitor);
        this.pessoaService = pessoaService;
    }

    @Override
    protected String titulo() {
        return "Cadastro de pessoas";
    }

    @Override
    protected List<String> opcoes() {
        return List.of("Cadastrar pessoa", "Listar pessoas", "Consultar pessoa", "Alterar pessoa", "Remover pessoa");
    }

    @Override
    protected void executarOpcao(int opcao) {
        switch (opcao) {
            case 1 -> cadastrar();
            case 2 -> listar();
            case 3 -> consultar();
            case 4 -> alterar();
            case 5 -> remover();
        }
    }

    private void cadastrar() {
        Pessoa pessoa = pessoaService.cadastrar(lerDados(null));
        IO.println("Pessoa cadastrada: " + pessoa);
    }

    private void listar() {
        List<Pessoa> pessoas = pessoaService.listar();
        if (pessoas.isEmpty()) {
            IO.println("Nenhuma pessoa cadastrada.");
            return;
        }
        pessoas.forEach(IO::println);
    }

    private void consultar() {
        exibirDetalhes(pessoaService.buscarPorId(leitor.lerInteiro("Id da pessoa")));
    }

    private void alterar() {
        int id = leitor.lerInteiro("Id da pessoa");
        Pessoa atual = pessoaService.buscarPorId(id);
        IO.println("Deixe em branco para manter o valor atual.");
        Pessoa pessoa = pessoaService.alterar(id, lerDados(atual));
        IO.println("Pessoa alterada: " + pessoa);
    }

    private void remover() {
        int id = leitor.lerInteiro("Id da pessoa");
        pessoaService.remover(id);
        IO.println("Pessoa removida.");
    }

    private DadosPessoa lerDados(Pessoa atual) {
        boolean novo = atual == null;
        String nome = leitor.lerTexto("Nome", novo ? null : atual.getNome());
        int idade = leitor.lerInteiro("Idade", novo ? null : atual.getIdade());
        Vinculo vinculo = leitor.lerOpcao("Vínculo", Vinculo.values(), novo ? null : atual.getVinculo());
        List<String> gostos = leitor.lerLista("Gostos", novo ? null : atual.getGostos());
        List<String> interesses = leitor.lerLista("Interesses", novo ? null : atual.getInteresses());
        List<String> naoGosta = leitor.lerLista("Não gosta", novo ? null : atual.getNaoGosta());
        Ocasiao ocasiao = leitor.lerOpcao("Ocasião", Ocasiao.values(), novo ? null : atual.getOcasiao());
        BigDecimal minimo = leitor.lerValor("Orçamento mínimo (R$)", novo ? null : atual.getOrcamento().minimo());
        BigDecimal maximo = leitor.lerValor("Orçamento máximo (R$)", novo ? null : atual.getOrcamento().maximo());
        return new DadosPessoa(nome, idade, vinculo, gostos, interesses, naoGosta, ocasiao,
                new FaixaOrcamento(minimo, maximo));
    }

    private void exibirDetalhes(Pessoa pessoa) {
        FaixaOrcamento orcamento = pessoa.getOrcamento();
        IO.println("Id: " + pessoa.getId());
        IO.println("Nome: " + pessoa.getNome());
        IO.println("Idade: " + pessoa.getIdade() + " anos");
        IO.println("Vínculo: " + pessoa.getVinculo());
        IO.println("Gostos: " + Formatador.lista(pessoa.getGostos()));
        IO.println("Interesses: " + Formatador.lista(pessoa.getInteresses()));
        IO.println("Não gosta: " + Formatador.lista(pessoa.getNaoGosta()));
        IO.println("Ocasião: " + pessoa.getOcasiao());
        IO.println("Orçamento: " + Formatador.moeda(orcamento.minimo()) + " a " + Formatador.moeda(orcamento.maximo()));
    }
}
