import { api } from "./api.js";
import { atualizarBotaoFavoritar, EstadoFavoritos } from "./favoritar.js";
import { botaoFavoritar, renderizarItem } from "./itens.js";
import { descricao } from "./opcoes.js";
import { nomeDaPessoa, preencherSeletorPessoas } from "./seletor-pessoas.js";
import { escaparHtml, formatarDataHora, formatarMoeda, mostrarAviso, mostrarErro } from "./util.js";

const filtroPessoa = document.getElementById("filtro-historico");
const lista = document.getElementById("lista-historico");
const favoritos = new EstadoFavoritos();
let pessoas = [];
let registros = [];

export function iniciarHistorico() {
    filtroPessoa.addEventListener("change", () => carregarHistorico().catch(mostrarErro));
    lista.addEventListener("click", tratarClique);
}

/** Chamado ao abrir a aba, pois novas sugestões podem ter sido geradas. */
export async function atualizarHistorico() {
    pessoas = await api.listarPessoas();
    preencherSeletorPessoas(filtroPessoa, pessoas, "Todas as pessoas");
    await carregarHistorico();
}

async function carregarHistorico() {
    [registros] = await Promise.all([api.listarHistorico(filtroPessoa.value), favoritos.carregar()]);
    renderizar();
}

function resumo(registro) {
    const quantidade = registro.itens.length;
    return quantidade === 0 ? "nenhuma sugestão encontrada"
        : quantidade === 1 ? "1 sugestão" : `${quantidade} sugestões`;
}

function renderizar() {
    if (registros.length === 0) {
        lista.innerHTML = `<p class="vazio">Nenhuma sugestão gerada ainda. Use a aba Sugestões.</p>`;
        return;
    }
    lista.innerHTML = registros.map((registro) => `
        <article class="cartao registro" data-id="${registro.id}">
            <div class="item__topo">
                <div class="item__info">
                    <h3 class="item__titulo">
                        ${escaparHtml(nomeDaPessoa(pessoas, registro.pessoaId))}
                        <span class="etiqueta etiqueta--origem-${registro.origem.toLowerCase()}">
                            ${descricao("origens", registro.origem)}
                        </span>
                    </h3>
                    <p class="item__subtitulo">
                        ${formatarDataHora(registro.realizadoEm)} · ${escaparHtml(descricao("ocasioes", registro.ocasiao))}
                        · até ${formatarMoeda(registro.orcamento.maximo)}
                    </p>
                </div>
                <span class="registro__resumo">${resumo(registro)}</span>
            </div>
            ${registro.itens.length ? `
                <div class="acoes">
                    <button type="button" class="botao botao--secundario botao--pequeno" data-acao="ver"
                            aria-expanded="false">Ver sugestões</button>
                </div>
                <div class="registro__itens grade-sugestoes" hidden></div>` : ""}
        </article>`).join("");
}

function registroDoElemento(elemento) {
    const id = Number(elemento.closest(".registro").dataset.id);
    return registros.find((registro) => registro.id === id);
}

async function tratarClique(evento) {
    const botao = evento.target.closest("button[data-acao]");
    if (!botao) {
        return;
    }
    if (botao.dataset.acao === "ver") {
        alternarItens(botao);
    } else if (botao.dataset.acao === "favoritar") {
        await favoritarItem(botao);
    }
}

function alternarItens(botao) {
    const registro = registroDoElemento(botao);
    const container = botao.closest(".registro").querySelector(".registro__itens");
    const abrir = container.hidden;
    if (abrir) {
        container.innerHTML = registro.itens.map((item, indice) => renderizarItem(item, registro.origem, {
            posicao: indice + 1,
            acoes: botaoFavoritar(indice, favoritos.estaFavoritado(registro.pessoaId, registro.origem, item)),
        })).join("");
    }
    container.hidden = !abrir;
    botao.setAttribute("aria-expanded", abrir);
    botao.textContent = abrir ? "Ocultar sugestões" : "Ver sugestões";
}

async function favoritarItem(botao) {
    const registro = registroDoElemento(botao);
    const item = registro.itens[Number(botao.dataset.indice)];
    botao.disabled = true;
    try {
        const favoritado = await favoritos.alternar(registro.pessoaId, registro.origem, item);
        atualizarBotaoFavoritar(botao, favoritado);
        mostrarAviso(favoritado ? "Adicionado aos favoritos." : "Removido dos favoritos.");
    } catch (erro) {
        mostrarErro(erro);
    } finally {
        botao.disabled = false;
    }
}
