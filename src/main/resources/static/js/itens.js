// Formato comum dos itens sugeridos (catálogo ou IA), usado em sugestões, histórico e favoritos.
import { descricao } from "./opcoes.js";
import { escaparHtml, etiquetas, formatarMoeda } from "./util.js";

export const CATALOGO = "CATALOGO";
export const IA = "IA";

export function itemDeSugestao({ presente, motivos }) {
    return {
        presenteId: presente.id,
        nome: presente.nome,
        categoria: descricao("categorias", presente.categoria),
        preco: presente.preco,
        motivos,
    };
}

export function itemDeIdeia(ideia) {
    return {
        presenteId: null,
        nome: ideia.nome,
        categoria: ideia.categoria,
        preco: ideia.precoEstimado,
        motivos: ideia.motivo ? [ideia.motivo] : [],
    };
}

export function botaoFavoritar(indice, favoritado) {
    return `
        <button type="button" class="botao botao--favorito botao--pequeno" data-acao="favoritar"
                data-indice="${indice}" aria-pressed="${favoritado}">
            ${favoritado ? "★ Favoritado" : "☆ Favoritar"}
        </button>`;
}

/**
 * @param opcoes.posicao número exibido no círculo; omitido, o círculo não aparece
 * @param opcoes.rodape HTML extra abaixo dos motivos
 * @param opcoes.acoes HTML dos botões
 */
export function renderizarItem(item, origem, { posicao, rodape = "", acoes = "" } = {}) {
    const ehIa = origem === IA;
    const motivos = ehIa
        ? item.motivos.map((motivo) => `<p class="item__descricao">${escaparHtml(motivo)}</p>`).join("")
        : `<p class="item__descricao">${etiquetas(item.motivos, "etiqueta--motivo")}</p>`;
    return `
        <article class="cartao item sugestao ${ehIa ? "sugestao--ia" : ""}">
            <div class="item__topo">
                ${posicao ? `<span class="sugestao__posicao">${posicao}</span>` : ""}
                <div class="item__info">
                    <h3 class="item__titulo">${escaparHtml(item.nome)}</h3>
                    <p class="item__subtitulo">
                        ${escaparHtml(item.categoria)}
                        <span class="etiqueta etiqueta--origem-${origem.toLowerCase()}">${descricao("origens", origem)}</span>
                    </p>
                </div>
                <span class="item__preco">${ehIa ? "≈ " : ""}${formatarMoeda(item.preco)}</span>
            </div>
            ${item.motivos.length ? motivos : ""}
            ${rodape}
            ${acoes ? `<div class="acoes">${acoes}</div>` : ""}
        </article>`;
}
