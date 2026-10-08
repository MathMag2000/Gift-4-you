import { api } from "./api.js";
import { ControleFormulario } from "./formulario.js";
import { descricao } from "./opcoes.js";
import {
    escaparHtml, etiquetas, formatarMoeda, listaParaTexto, mostrarAviso, mostrarErro,
    numeroOuNulo, textoOuNulo, textoParaLista,
} from "./util.js";

const formulario = document.getElementById("form-pessoa");
const lista = document.getElementById("lista-pessoas");
const controle = new ControleFormulario(formulario, document.getElementById("titulo-form-pessoa"), "pessoa");
let pessoas = [];

export function iniciarPessoas() {
    formulario.addEventListener("submit", salvar);
    lista.addEventListener("click", tratarAcaoDaLista);
    return carregar();
}

async function carregar() {
    pessoas = await api.listarPessoas();
    renderizar();
}

function lerFormulario() {
    const campos = formulario.elements;
    return {
        nome: campos.nome.value,
        idade: numeroOuNulo(campos.idade.value),
        vinculo: textoOuNulo(campos.vinculo.value),
        gostos: textoParaLista(campos.gostos.value),
        interesses: textoParaLista(campos.interesses.value),
        naoGosta: textoParaLista(campos.naoGosta.value),
        ocasiao: textoOuNulo(campos.ocasiao.value),
        orcamento: { maximo: numeroOuNulo(campos.orcamentoMaximo.value) },
    };
}

function preencherFormulario(pessoa) {
    const campos = formulario.elements;
    campos.nome.value = pessoa.nome;
    campos.idade.value = pessoa.idade;
    campos.vinculo.value = pessoa.vinculo;
    campos.gostos.value = listaParaTexto(pessoa.gostos);
    campos.interesses.value = listaParaTexto(pessoa.interesses);
    campos.naoGosta.value = listaParaTexto(pessoa.naoGosta);
    campos.ocasiao.value = pessoa.ocasiao;
    campos.orcamentoMaximo.value = pessoa.orcamento.maximo;
}

async function salvar(evento) {
    evento.preventDefault();
    await controle.executarSalvando(async () => {
        try {
            const dados = lerFormulario();
            if (controle.emEdicao()) {
                await api.alterarPessoa(controle.idEmEdicao, dados);
                mostrarAviso("Pessoa alterada.");
            } else {
                await api.cadastrarPessoa(dados);
                mostrarAviso("Pessoa cadastrada.");
            }
            controle.voltarParaCadastro();
            await carregar();
        } catch (erro) {
            mostrarErro(erro);
        }
    });
}

async function tratarAcaoDaLista(evento) {
    const botao = evento.target.closest("button[data-acao]");
    if (!botao) {
        return;
    }
    const pessoa = pessoas.find((item) => item.id === Number(botao.dataset.id));
    if (botao.dataset.acao === "editar") {
        preencherFormulario(pessoa);
        controle.editar(pessoa.id);
    } else if (botao.dataset.acao === "remover") {
        await remover(pessoa);
    }
}

async function remover(pessoa) {
    if (!confirm(`Remover ${pessoa.nome}?`)) {
        return;
    }
    try {
        await api.removerPessoa(pessoa.id);
        if (controle.idEmEdicao === pessoa.id) {
            controle.voltarParaCadastro();
        }
        mostrarAviso("Pessoa removida.");
        await carregar();
    } catch (erro) {
        mostrarErro(erro);
    }
}

function renderizar() {
    if (pessoas.length === 0) {
        lista.innerHTML = `<p class="vazio">Nenhuma pessoa cadastrada ainda.</p>`;
        return;
    }
    lista.innerHTML = pessoas.map((pessoa) => `
        <article class="cartao item">
            <div class="item__topo">
                <div>
                    <h3 class="item__titulo">${escaparHtml(pessoa.nome)}</h3>
                    <p class="item__subtitulo">
                        ${escaparHtml(descricao("vinculos", pessoa.vinculo))} · ${pessoa.idade} anos
                    </p>
                </div>
                <span class="item__preco">até ${formatarMoeda(pessoa.orcamento.maximo)}</span>
            </div>
            <dl class="item__detalhes">
                <dt>Ocasião</dt><dd>${escaparHtml(descricao("ocasioes", pessoa.ocasiao))}</dd>
                <dt>Gostos</dt><dd>${etiquetas(pessoa.gostos)}</dd>
                <dt>Interesses</dt><dd>${etiquetas(pessoa.interesses)}</dd>
                <dt>Não gosta</dt><dd>${etiquetas(pessoa.naoGosta, "etiqueta--negativa")}</dd>
            </dl>
            <div class="acoes">
                <button type="button" class="botao botao--secundario botao--pequeno"
                        data-acao="editar" data-id="${pessoa.id}">Editar</button>
                <button type="button" class="botao botao--perigo botao--pequeno"
                        data-acao="remover" data-id="${pessoa.id}">Remover</button>
            </div>
        </article>`).join("");
}
