package model;

public enum Categoria {

    ELETRONICOS("Eletrônicos"),
    LIVROS("Livros"),
    MODA("Roupas e acessórios"),
    BELEZA("Beleza e cuidados pessoais"),
    CASA("Casa e decoração"),
    ESPORTES("Esportes"),
    BRINQUEDOS("Brinquedos"),
    JOGOS("Jogos"),
    MUSICA("Música"),
    GASTRONOMIA("Gastronomia"),
    EXPERIENCIAS("Experiências"),
    ARTE("Arte e artesanato"),
    OUTRA("Outra");

    private final String descricao;

    Categoria(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
