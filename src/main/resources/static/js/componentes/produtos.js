// Cartões de produto (com coração para favoritar) e a janela de detalhes do produto.
import { descricao } from "../opcoes.js";
import { IA, precoEstimado } from "../itens.js";
import {
    enderecoParaExibir, escaparHtml, formatarMoeda, imagemProduto, mostrarAviso, mostrarErro, urlSegura,
} from "../util.js";
import { icones } from "./icones.js";
import { abrirModal } from "./modal.js";

/**
 * Uma entrada exibida num cartão.
 * @typedef {{ item: object, origem: string, pessoaId: number|null, nota?: string }} Entrada
 */

function preco(entrada) {
    return `${precoEstimado(entrada.origem, entrada.item) ? "≈ " : ""}${formatarMoeda(entrada.item.preco)}`;
}

function botaoCoracao(favoritado, tamanho, classe) {
    return `<button type="button" class="${classe} ${favoritado ? "coracao--ativo" : ""}" data-acao="favoritar"
                    aria-pressed="${favoritado}" aria-label="${favoritado ? "Remover dos favoritos" : "Favoritar"}"
                    title="${favoritado ? "Remover dos favoritos" : "Favoritar"}">
                ${icones.coracao(tamanho, favoritado)}
            </button>`;
}

function cartao(entrada, indice, favoritos) {
    const { item, origem, pessoaId } = entrada;
    const favoritavel = pessoaId !== null && pessoaId !== undefined;
    const favoritado = favoritavel && favoritos.estaFavoritado(pessoaId, origem, item);
    return `
        <article class="produto" data-indice="${indice}" tabindex="0" role="button"
                 aria-label="Ver detalhes de ${escaparHtml(item.nome)}">
            <div class="produto__topo">
                ${origem === IA ? `<span class="selo">IA</span>` : "<span></span>"}
                ${favoritavel ? botaoCoracao(favoritado, 20, "coracao") : ""}
            </div>
            <div class="produto__imagem">${imagemProduto(item.imagemUrl, item.nome, "produto__foto")}</div>
            <div class="produto__info">
                <p class="produto__nome">${escaparHtml(item.nome)}</p>
                <p class="produto__preco">${preco(entrada)}</p>
                ${item.categoria ? `<p class="produto__categoria">${escaparHtml(item.categoria)}</p>` : ""}
                ${entrada.nota ? `<p class="produto__nota">${escaparHtml(entrada.nota)}</p>` : ""}
            </div>
        </article>`;
}

/**
 * Mostra os cartões no elemento e trata os cliques: o coração favorita, o restante abre os detalhes.
 *
 * @param {Entrada[]} entradas
 * @param opcoes.aoAlternarFavorito chamado depois de favoritar ou desfavoritar (ex.: para recarregar a lista)
 */
export function renderizarProdutos(elemento, entradas, favoritos, { aoAlternarFavorito } = {}) {
    elemento.innerHTML = entradas.map((entrada, indice) => cartao(entrada, indice, favoritos)).join("");

    const entradaDo = (alvo) => entradas[Number(alvo.closest(".produto").dataset.indice)];

    async function alternar(botao, entrada) {
        botao.disabled = true;
        try {
            const favoritado = await favoritos.alternar(entrada.pessoaId, entrada.origem, entrada.item);
            mostrarAviso(favoritado ? "Adicionado aos favoritos." : "Removido dos favoritos.");
            if (aoAlternarFavorito) {
                await aoAlternarFavorito();
            } else {
                renderizarProdutos(elemento, entradas, favoritos, { aoAlternarFavorito });
            }
        } catch (erro) {
            mostrarErro(erro);
            botao.disabled = false;
        }
    }

    elemento.onclick = (evento) => {
        const produto = evento.target.closest(".produto");
        if (!produto) {
            return;
        }
        const botao = evento.target.closest('[data-acao="favoritar"]');
        if (botao) {
            alternar(botao, entradaDo(botao));
        } else {
            abrirDetalhes(entradaDo(produto), favoritos,
                () => renderizarProdutos(elemento, entradas, favoritos, { aoAlternarFavorito }), aoAlternarFavorito);
        }
    };
    elemento.onkeydown = (evento) => {
        if ((evento.key === "Enter" || evento.key === " ") && evento.target.classList.contains("produto")) {
            evento.preventDefault();
            evento.target.click();
        }
    };
}

function abrirDetalhes(entrada, favoritos, redesenharCartoes, aoAlternarFavorito) {
    const { item, origem, pessoaId } = entrada;
    const favoritavel = pessoaId !== null && pessoaId !== undefined;
    const link = urlSegura(item.linkCompra);
    const ehBusca = link && /\/s\?k=/.test(link);

    function conteudo() {
        const favoritado = favoritavel && favoritos.estaFavoritado(pessoaId, origem, item);
        return `
            <div class="modal__cabecalho">
                <span>Detalhes do produto</span>
                <button type="button" class="modal__fechar" data-acao="fechar" aria-label="Fechar">✕</button>
            </div>
            <div class="detalhe-produto">
                <div class="detalhe-produto__imagem">${imagemProduto(item.imagemUrl, item.nome, "detalhe-produto__foto")}</div>
                <div class="detalhe-produto__info">
                    <div>
                        <h2 class="detalhe-produto__nome">${escaparHtml(item.nome)}</h2>
                        <p class="detalhe-produto__categoria">
                            ${escaparHtml(item.categoria || "")}
                            <span class="selo">${escaparHtml(descricao("origens", origem))}</span>
                        </p>
                    </div>
                    <div>
                        <p class="detalhe-produto__preco">${preco(entrada)}</p>
                        ${precoEstimado(origem, item)
                            ? `<p class="detalhe-produto__aviso">Preço estimado pela IA; confira na loja.</p>` : ""}
                    </div>
                    ${item.descricao ? `
                        <div class="detalhe-produto__secao">
                            <p class="rotulo-secao">Descrição</p>
                            <p>${escaparHtml(item.descricao)}</p>
                        </div>` : ""}
                    ${item.motivos?.length ? `
                        <div class="detalhe-produto__secao">
                            <p class="rotulo-secao">Por que combina</p>
                            <ul class="lista-motivos">${item.motivos.map((motivo) => `<li>${escaparHtml(motivo)}</li>`).join("")}</ul>
                        </div>` : ""}
                    ${link ? `
                        <div class="detalhe-produto__secao">
                            <p class="rotulo-secao">Loja</p>
                            <a class="link-loja" href="${escaparHtml(link)}" target="_blank" rel="noopener noreferrer">
                                <span>${escaparHtml(enderecoParaExibir(link))}</span>${icones.externo()}
                            </a>
                        </div>` : ""}
                    <div class="acoes">
                        ${link ? `<a class="botao" href="${escaparHtml(link)}" target="_blank" rel="noopener noreferrer">
                                      ${ehBusca ? "Buscar na loja →" : "Acessar loja →"}</a>` : ""}
                        ${favoritavel ? `
                            <button type="button" class="botao botao--contorno ${favoritado ? "botao--marcado" : ""}"
                                    data-acao="favoritar" aria-pressed="${favoritado}">
                                ${icones.coracao(14, favoritado)} ${favoritado ? "Favoritado" : "Favoritar"}
                            </button>` : ""}
                    </div>
                </div>
            </div>`;
    }

    const modal = abrirModal(conteudo(), { classe: "modal--produto", rotulo: `Detalhes de ${item.nome}` });
    modal.caixa.addEventListener("click", async (evento) => {
        const botao = evento.target.closest('[data-acao="favoritar"]');
        if (!botao) {
            return;
        }
        botao.disabled = true;
        try {
            const favoritado = await favoritos.alternar(pessoaId, origem, item);
            mostrarAviso(favoritado ? "Adicionado aos favoritos." : "Removido dos favoritos.");
            modal.caixa.innerHTML = conteudo();
            if (aoAlternarFavorito) {
                await aoAlternarFavorito();
            } else {
                redesenharCartoes();
            }
        } catch (erro) {
            mostrarErro(erro);
            botao.disabled = false;
        }
    });
}
