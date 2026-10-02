package ui;

import model.Categoria;
import model.DadosPresente;
import model.Ocasiao;
import model.Presente;
import service.PresenteService;

import java.math.BigDecimal;
import java.util.List;

public class MenuPresentes extends Menu {

    private final PresenteService presenteService;

    public MenuPresentes(PresenteService presenteService, LeitorConsole leitor) {
        super(leitor);
        this.presenteService = presenteService;
    }

    @Override
    protected String titulo() {
        return "Cadastro de presentes";
    }

    @Override
    protected List<String> opcoes() {
        return List.of("Cadastrar presente", "Listar presentes", "Consultar presente",
                "Alterar presente", "Remover presente");
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
        Presente presente = presenteService.cadastrar(lerDados(null));
        IO.println("Presente cadastrado: " + presente);
    }

    private void listar() {
        List<Presente> presentes = presenteService.listar();
        if (presentes.isEmpty()) {
            IO.println("Nenhum presente cadastrado.");
            return;
        }
        presentes.forEach(presente -> IO.println(presente + " - " + Formatador.moeda(presente.getPreco())));
    }

    private void consultar() {
        exibirDetalhes(presenteService.buscarPorId(leitor.lerInteiro("Id do presente")));
    }

    private void alterar() {
        int id = leitor.lerInteiro("Id do presente");
        Presente atual = presenteService.buscarPorId(id);
        IO.println("Deixe em branco para manter o valor atual.");
        Presente presente = presenteService.alterar(id, lerDados(atual));
        IO.println("Presente alterado: " + presente);
    }

    private void remover() {
        int id = leitor.lerInteiro("Id do presente");
        presenteService.remover(id);
        IO.println("Presente removido.");
    }

    private DadosPresente lerDados(Presente atual) {
        boolean novo = atual == null;
        String nome = leitor.lerTexto("Nome", novo ? null : atual.getNome());
        Categoria categoria = leitor.lerOpcao("Categoria", Categoria.values(), novo ? null : atual.getCategoria());
        String descricao = leitor.lerTextoOpcional("Descrição (opcional)", novo ? null : atual.getDescricao());
        BigDecimal preco = leitor.lerValor("Preço (R$)", novo ? null : atual.getPreco());
        List<String> caracteristicas = leitor.lerLista("Características", novo ? null : atual.getCaracteristicas());
        List<Ocasiao> ocasioes = leitor.lerOpcoes("Ocasiões compatíveis", Ocasiao.values(),
                novo ? null : atual.getOcasioes());
        return new DadosPresente(nome, categoria, descricao, preco, caracteristicas, ocasioes);
    }

    private void exibirDetalhes(Presente presente) {
        IO.println("Id: " + presente.getId());
        IO.println("Nome: " + presente.getNome());
        IO.println("Categoria: " + presente.getCategoria());
        IO.println("Descrição: " + (presente.getDescricao().isEmpty() ? "-" : presente.getDescricao()));
        IO.println("Preço: " + Formatador.moeda(presente.getPreco()));
        IO.println("Características: " + Formatador.lista(presente.getCaracteristicas()));
        IO.println("Ocasiões compatíveis: " + Formatador.lista(presente.getOcasioes()));
    }
}
