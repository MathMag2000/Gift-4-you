// Peças dos formulários no estilo do wireframe: blocos com título, campos e tags selecionáveis.
import { capitalizar, escaparHtml } from "../util.js";

/** Cartão com título em maiúsculas, como "SOBRE A PESSOA". */
export function bloco(titulo, conteudo, ajuda = "") {
    return `
        <section class="cartao bloco">
            <h2 class="bloco__titulo">${titulo}</h2>
            ${ajuda ? `<p class="bloco__ajuda">${ajuda}</p>` : ""}
            ${conteudo}
        </section>`;
}

export function campo(rotulo, entrada, classeExtra = "") {
    return `<label class="campo ${classeExtra}"><span class="campo__rotulo">${rotulo}</span>${entrada}</label>`;
}

/**
 * Tags selecionáveis. São caixas de seleção (ou botões de opção) de verdade, estilizadas como etiquetas,
 * para funcionarem com teclado e leitores de tela.
 *
 * @param opcoes lista de { valor, descricao }
 * @param tipo "checkbox" para várias escolhas, "radio" para uma só
 */
export function tags(nome, opcoes, selecionados, { tipo = "checkbox", rotulo = "" } = {}) {
    const itens = opcoes.map(({ valor, descricao }) => `
        <label class="tag">
            <input type="${tipo}" name="${nome}" value="${escaparHtml(valor)}"
                   ${selecionados.includes(valor) ? "checked" : ""}>
            <span>${escaparHtml(capitalizar(descricao))}</span>
        </label>`).join("");
    return `<div class="tags" role="group" ${rotulo ? `aria-label="${escaparHtml(rotulo)}"` : ""}>${itens}</div>`;
}

export function valoresMarcados(formulario, nome) {
    return [...formulario.querySelectorAll(`input[name="${nome}"]:checked`)].map((entrada) => entrada.value);
}

export function valorMarcado(formulario, nome) {
    return formulario.querySelector(`input[name="${nome}"]:checked`)?.value ?? null;
}

export function botaoVoltar(destino, texto) {
    return `<a class="voltar" href="${destino}">← ${escaparHtml(texto)}</a>`;
}

export function cabecalhoPagina(titulo, subtitulo = "", acoes = "") {
    return `
        <header class="cabecalho-pagina">
            <div>
                <h1 class="titulo-pagina">${titulo}</h1>
                ${subtitulo ? `<p class="subtitulo-pagina">${subtitulo}</p>` : ""}
            </div>
            ${acoes ? `<div class="cabecalho-pagina__acoes">${acoes}</div>` : ""}
        </header>`;
}

export function vazio(icone, texto, acao = "") {
    return `<div class="vazio">${icone}<p>${texto}</p>${acao}</div>`;
}

export function seletorPessoas(id, pessoas, selecionada, textoTodas = "Todas as pessoas") {
    const opcoes = pessoas.map((pessoa) =>
        `<option value="${pessoa.id}" ${String(pessoa.id) === String(selecionada) ? "selected" : ""}>${escaparHtml(pessoa.nome)}</option>`);
    return `
        <label class="campo filtro">
            <span class="campo__rotulo">Pessoa</span>
            <select id="${id}"><option value="">${escaparHtml(textoTodas)}</option>${opcoes.join("")}</select>
        </label>`;
}

export function nomeDaPessoa(pessoas, pessoaId) {
    return pessoas.find((pessoa) => pessoa.id === pessoaId)?.nome ?? "Pessoa removida";
}
