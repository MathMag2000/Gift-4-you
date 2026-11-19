// Formato comum dos itens sugeridos (catálogo ou IA), usado em sugestões, histórico e favoritos.
import { descricao } from "./opcoes.js";

export const CATALOGO = "CATALOGO";
export const IA = "IA";

export function itemDePresente(presente, motivos = []) {
    return {
        presenteId: presente.id,
        nome: presente.nome,
        categoria: descricao("categorias", presente.categoria),
        preco: presente.preco,
        motivos,
        linkCompra: presente.linkCompra,
        imagemUrl: presente.imagemUrl,
        descricao: presente.descricao,
    };
}

export function itemDeSugestao({ presente, motivos }) {
    return itemDePresente(presente, motivos);
}

export function itemDeIdeia(ideia) {
    return {
        presenteId: null,
        nome: ideia.nome,
        categoria: ideia.categoria,
        preco: ideia.precoEstimado,
        motivos: ideia.motivo ? [ideia.motivo] : [],
        linkCompra: ideia.linkCompra,
        imagemUrl: ideia.imagemUrl,
    };
}

/** Ideias da IA não encontradas na loja ficam sem foto, com o preço estimado pela IA. */
export function precoEstimado(origem, item) {
    return origem === IA && !item.imagemUrl;
}
