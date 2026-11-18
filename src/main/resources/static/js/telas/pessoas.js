import { api } from "../api.js";
import { icones } from "../componentes/icones.js";
import { confirmar } from "../componentes/modal.js";
import { botaoVoltar, cabecalhoPagina, vazio } from "../componentes/formulario.js";
import { descricao } from "../opcoes.js";
import { capitalizar, escaparHtml, formatarMoeda, listaParaTexto, mostrarAviso, mostrarErro } from "../util.js";

export async function telaPessoas(principal) {
    const pessoas = await api.listarPessoas();
    const lista = pessoas.length === 0
        ? vazio(icones.pessoa(32), "Nenhuma pessoa cadastrada ainda.<br>Cadastre alguém para receber sugestões.")
        : `<div class="lista">${pessoas.map((pessoa) => `
            <a class="cartao linha-pessoa" href="#/pessoas/${pessoa.id}">
                <div class="linha-pessoa__info">
                    <span class="linha-pessoa__nome">${escaparHtml(pessoa.nome)}</span>
                    <span class="linha-pessoa__dados">
                        ${pessoa.idade} anos · ${escaparHtml(descricao("vinculos", pessoa.vinculo))}
                    </span>
                    ${pessoa.gostos.length
                        ? `<span class="linha-pessoa__gostos">${escaparHtml(capitalizar(listaParaTexto(pessoa.gostos)))}</span>`
                        : ""}
                </div>
                <span class="linha-pessoa__seta">${icones.direita()}</span>
            </a>`).join("")}</div>`;

    principal.innerHTML = `
        ${cabecalhoPagina("Pessoas", "Gerencie as pessoas e encontre sugestões personalizadas para cada uma.",
            `<a class="botao" href="#/pessoas/nova">+ Cadastrar nova pessoa</a>`)}
        ${lista}`;
}

function linhaDetalhe(rotulo, valor) {
    return `<div class="detalhe"><dt>${rotulo}</dt><dd>${valor || "—"}</dd></div>`;
}

function textoLista(lista) {
    return escaparHtml(capitalizar(listaParaTexto(lista)));
}

export async function telaDetalhesPessoa(principal, { id }) {
    const pessoa = await api.buscarPessoa(id);
    principal.innerHTML = `
        ${botaoVoltar("#/pessoas", "Pessoas")}
        <h1 class="titulo-pagina">${escaparHtml(pessoa.nome)}</h1>
        <dl class="cartao detalhes">
            ${linhaDetalhe("Idade", `${pessoa.idade} anos`)}
            ${linhaDetalhe("Vínculo", escaparHtml(descricao("vinculos", pessoa.vinculo)))}
            ${linhaDetalhe("Gostos e interesses", textoLista(pessoa.gostos))}
            ${linhaDetalhe("Não gosta", textoLista(pessoa.naoGosta))}
            ${linhaDetalhe("Estilo / preferências", textoLista(pessoa.estilos))}
            ${linhaDetalhe("Ocasião", escaparHtml(descricao("ocasioes", pessoa.ocasiao)))}
            ${linhaDetalhe("Orçamento", `até ${formatarMoeda(pessoa.orcamento.maximo)}`)}
            ${linhaDetalhe("Informações adicionais", escaparHtml(pessoa.observacoes))}
        </dl>
        <div class="acoes">
            <a class="botao" href="#/pessoas/${pessoa.id}/sugestoes">Ver sugestões →</a>
            <a class="botao botao--contorno-forte" href="#/pessoas/${pessoa.id}/editar">Editar pessoa</a>
            <button type="button" class="botao botao--remover" data-acao="remover">Remover pessoa</button>
        </div>
        <p class="links-pessoa">
            <a class="link-discreto" href="#/favoritos?pessoa=${pessoa.id}">Favoritos de ${escaparHtml(pessoa.nome)}</a>
            <a class="link-discreto" href="#/historico?pessoa=${pessoa.id}">Histórico de sugestões</a>
        </p>`;

    principal.querySelector('[data-acao="remover"]').addEventListener("click", async () => {
        const confirmado = await confirmar({
            titulo: "Deseja realmente remover esta pessoa?",
            texto: "Esta ação não poderá ser desfeita. Os favoritos e o histórico dela também serão apagados.",
        });
        if (!confirmado) {
            return;
        }
        try {
            await api.removerPessoa(pessoa.id);
            mostrarAviso(`${pessoa.nome} foi removida.`);
            location.hash = "#/pessoas";
        } catch (erro) {
            mostrarErro(erro);
        }
    });
}
