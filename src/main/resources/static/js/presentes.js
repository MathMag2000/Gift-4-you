import { api } from "./api.js";
import { ControleFormulario } from "./formulario.js";
import { descricao, preencherMarcadores } from "./opcoes.js";
import {
    escaparHtml, etiquetas, formatarMoeda, listaParaTexto, mostrarAviso, mostrarErro,
    numeroOuNulo, textoOuNulo, textoParaLista,
} from "./util.js";

const formulario = document.getElementById("form-presente");
const lista = document.getElementById("lista-presentes");
const controle = new ControleFormulario(formulario, document.getElementById("titulo-form-presente"), "presente");
let presentes = [];

export function iniciarPresentes() {
    preencherMarcadores(document.getElementById("ocasioes-presente"), "ocasioes", "ocasioes");
    formulario.addEventListener("submit", salvar);
    lista.addEventListener("click", tratarAcaoDaLista);
    return carregar();
}

async function carregar() {
    presentes = await api.listarPresentes();
    renderizar();
}

function ocasioesMarcadas() {
    return [...formulario.querySelectorAll('input[name="ocasioes"]:checked')].map((marcador) => marcador.value);
}

function lerFormulario() {
    const campos = formulario.elements;
    return {
        nome: campos.nome.value,
        categoria: textoOuNulo(campos.categoria.value),
        descricao: campos.descricao.value,
        preco: numeroOuNulo(campos.preco.value),
        caracteristicas: textoParaLista(campos.caracteristicas.value),
        ocasioes: ocasioesMarcadas(),
    };
}

function preencherFormulario(presente) {
    const campos = formulario.elements;
    campos.nome.value = presente.nome;
    campos.categoria.value = presente.categoria;
    campos.descricao.value = presente.descricao;
    campos.preco.value = presente.preco;
    campos.caracteristicas.value = listaParaTexto(presente.caracteristicas);
    formulario.querySelectorAll('input[name="ocasioes"]').forEach((marcador) => {
        marcador.checked = presente.ocasioes.includes(marcador.value);
    });
}

async function salvar(evento) {
    evento.preventDefault();
    await controle.executarSalvando(async () => {
        try {
            const dados = lerFormulario();
            if (controle.emEdicao()) {
                await api.alterarPresente(controle.idEmEdicao, dados);
                mostrarAviso("Presente alterado.");
            } else {
                await api.cadastrarPresente(dados);
                mostrarAviso("Presente cadastrado.");
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
    const presente = presentes.find((item) => item.id === Number(botao.dataset.id));
    if (botao.dataset.acao === "editar") {
        preencherFormulario(presente);
        controle.editar(presente.id);
    } else if (botao.dataset.acao === "remover") {
        await remover(presente);
    }
}

async function remover(presente) {
    if (!confirm(`Remover ${presente.nome}?`)) {
        return;
    }
    try {
        await api.removerPresente(presente.id);
        if (controle.idEmEdicao === presente.id) {
            controle.voltarParaCadastro();
        }
        mostrarAviso("Presente removido.");
        await carregar();
    } catch (erro) {
        mostrarErro(erro);
    }
}

function renderizar() {
    if (presentes.length === 0) {
        lista.innerHTML = `<p class="vazio">Nenhum presente no catálogo ainda.</p>`;
        return;
    }
    lista.innerHTML = presentes.map((presente) => `
        <article class="cartao item">
            <div class="item__topo">
                <div>
                    <h3 class="item__titulo">${escaparHtml(presente.nome)}</h3>
                    <p class="item__subtitulo">${escaparHtml(descricao("categorias", presente.categoria))}</p>
                </div>
                <span class="item__preco">${formatarMoeda(presente.preco)}</span>
            </div>
            ${presente.descricao ? `<p class="item__descricao">${escaparHtml(presente.descricao)}</p>` : ""}
            <dl class="item__detalhes">
                <dt>Características</dt><dd>${etiquetas(presente.caracteristicas)}</dd>
                <dt>Ocasiões</dt>
                <dd>${etiquetas(presente.ocasioes.map((ocasiao) => descricao("ocasioes", ocasiao)))}</dd>
            </dl>
            <div class="acoes">
                <button type="button" class="botao botao--secundario botao--pequeno"
                        data-acao="editar" data-id="${presente.id}">Editar</button>
                <button type="button" class="botao botao--perigo botao--pequeno"
                        data-acao="remover" data-id="${presente.id}">Remover</button>
            </div>
        </article>`).join("");
}
