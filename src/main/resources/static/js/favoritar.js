// Estado dos favoritos usado pelos botões "Favoritar" nas sugestões e no histórico.
import { api } from "./api.js";
import { CATALOGO } from "./itens.js";

function mesmoItem(favorito, pessoaId, origem, item) {
    if (favorito.pessoaId !== pessoaId || favorito.origem !== origem) {
        return false;
    }
    return origem === CATALOGO
        ? favorito.item.presenteId === item.presenteId
        : favorito.item.nome.toLowerCase() === item.nome.trim().toLowerCase();
}

export class EstadoFavoritos {

    constructor() {
        this.favoritos = [];
    }

    /** @param pessoaId se omitido, carrega os favoritos de todas as pessoas */
    async carregar(pessoaId) {
        this.favoritos = await api.listarFavoritos(pessoaId);
    }

    estaFavoritado(pessoaId, origem, item) {
        return this.favoritos.some((favorito) => mesmoItem(favorito, pessoaId, origem, item));
    }

    /** Favorita ou desfavorita o item e devolve se ele ficou favoritado. */
    async alternar(pessoaId, origem, item) {
        const existente = this.favoritos.find((favorito) => mesmoItem(favorito, pessoaId, origem, item));
        if (existente) {
            await api.removerFavorito(existente.id);
            this.favoritos = this.favoritos.filter((favorito) => favorito !== existente);
            return false;
        }
        this.favoritos.push(await api.favoritar({ pessoaId, origem, item }));
        return true;
    }
}

/** Atualiza o botão depois de alternar, sem redesenhar a lista inteira. */
export function atualizarBotaoFavoritar(botao, favoritado) {
    botao.setAttribute("aria-pressed", favoritado);
    botao.textContent = favoritado ? "★ Favoritado" : "☆ Favoritar";
}
