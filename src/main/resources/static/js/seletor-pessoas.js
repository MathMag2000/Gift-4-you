import { escaparHtml } from "./util.js";

/**
 * Preenche um <select> com as pessoas, mantendo a escolha anterior quando ela ainda existir.
 * @param textoTodas se informado, adiciona a opção "todas as pessoas" com valor vazio
 */
export function preencherSeletorPessoas(seletor, pessoas, textoTodas) {
    const selecionada = seletor.value;
    const opcoes = pessoas.map((pessoa) => `<option value="${pessoa.id}">${escaparHtml(pessoa.nome)}</option>`);
    if (textoTodas) {
        opcoes.unshift(`<option value="">${escaparHtml(textoTodas)}</option>`);
    } else if (pessoas.length === 0) {
        opcoes.push(`<option value="">Cadastre uma pessoa primeiro</option>`);
    }
    seletor.innerHTML = opcoes.join("");
    if ([...seletor.options].some((opcao) => opcao.value === selecionada)) {
        seletor.value = selecionada;
    }
}

export function nomeDaPessoa(pessoas, pessoaId) {
    return pessoas.find((pessoa) => pessoa.id === pessoaId)?.nome ?? "Pessoa removida";
}
