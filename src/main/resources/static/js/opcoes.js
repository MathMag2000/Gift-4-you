// Valores aceitos em vínculo, ocasião, categoria, gostos e estilos, carregados uma vez do servidor.
import { api } from "./api.js";

let opcoes;

export async function carregarOpcoes() {
    opcoes = await api.opcoes();
}

/** @returns {{ valor: string, descricao: string }[]} */
export function listaDeOpcoes(tipo) {
    return opcoes[tipo];
}

export function descricao(tipo, valor) {
    return opcoes[tipo].find((opcao) => opcao.valor === valor)?.descricao ?? valor;
}

/** Valores que não estão entre as opções do tipo (ex.: gostos digitados pelo usuário). */
export function valoresForaDasOpcoes(tipo, valores) {
    return valores.filter((valor) => !opcoes[tipo].some((opcao) => opcao.valor === valor));
}
