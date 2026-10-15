// Valores aceitos em vínculo, ocasião e categoria, carregados uma vez do servidor.
import { api } from "./api.js";
import { escaparHtml } from "./util.js";

let opcoes;

export async function carregarOpcoes() {
    opcoes = await api.opcoes();
}

export function descricao(tipo, valor) {
    return opcoes[tipo].find((opcao) => opcao.valor === valor)?.descricao ?? valor;
}

/** Preenche todos os <select data-opcoes="tipo"> da página. */
export function preencherSelects() {
    document.querySelectorAll("select[data-opcoes]").forEach((select) => {
        const itens = opcoes[select.dataset.opcoes]
            .map((opcao) => `<option value="${opcao.valor}">${escaparHtml(opcao.descricao)}</option>`)
            .join("");
        select.innerHTML = `<option value="">Selecione...</option>${itens}`;
    });
}

export function valoresMarcados(formulario, nomeCampo) {
    return [...formulario.querySelectorAll(`input[name="${nomeCampo}"]:checked`)].map((marcador) => marcador.value);
}

export function marcarValores(formulario, nomeCampo, valores) {
    formulario.querySelectorAll(`input[name="${nomeCampo}"]`).forEach((marcador) => {
        marcador.checked = valores.includes(marcador.value);
    });
}

/** Valores que não estão entre as opções do tipo (ex.: gostos digitados pelo usuário). */
export function valoresForaDasOpcoes(tipo, valores) {
    return valores.filter((valor) => !opcoes[tipo].some((opcao) => opcao.valor === valor));
}

export function preencherMarcadores(container, tipo, nomeCampo) {
    container.innerHTML = opcoes[tipo]
        .map((opcao) => `
            <label>
                <input type="checkbox" name="${nomeCampo}" value="${opcao.valor}">
                ${escaparHtml(opcao.descricao)}
            </label>`)
        .join("");
}
