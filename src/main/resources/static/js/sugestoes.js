import { api } from "./api.js";
import { descricao } from "./opcoes.js";
import { escaparHtml, etiquetas, formatarMoeda, mostrarErro } from "./util.js";

const seletorPessoa = document.getElementById("pessoa-sugestao");
const resultado = document.getElementById("resultado-sugestoes");
const botaoCatalogo = document.getElementById("botao-sugestoes-catalogo");
const botaoIa = document.getElementById("botao-ideias-ia");
let pessoas = [];

export function iniciarSugestoes() {
    botaoCatalogo.addEventListener("click", sugerirDoCatalogo);
    botaoIa.addEventListener("click", sugerirComIa);
}

/** Recarrega as pessoas, pois podem ter sido alteradas na aba Pessoas. */
export async function atualizarPessoas() {
    const selecionada = seletorPessoa.value;
    pessoas = await api.listarPessoas();
    seletorPessoa.innerHTML = pessoas.length === 0
        ? `<option value="">Cadastre uma pessoa primeiro</option>`
        : pessoas.map((pessoa) => `<option value="${pessoa.id}">${escaparHtml(pessoa.nome)}</option>`).join("");
    if (pessoas.some((pessoa) => String(pessoa.id) === selecionada)) {
        seletorPessoa.value = selecionada;
    }
    botaoCatalogo.disabled = botaoIa.disabled = pessoas.length === 0;
}

function pessoaSelecionada() {
    return pessoas.find((pessoa) => String(pessoa.id) === seletorPessoa.value);
}

function cabecalho(pessoa, titulo) {
    const ocasiao = descricao("ocasioes", pessoa.ocasiao);
    const orcamento = `${formatarMoeda(pessoa.orcamento.minimo)} a ${formatarMoeda(pessoa.orcamento.maximo)}`;
    return `
        <div class="resultado__cabecalho">
            <h2>${titulo} para ${escaparHtml(pessoa.nome)}</h2>
            <p>${escaparHtml(ocasiao)} · orçamento de ${orcamento}</p>
        </div>`;
}

async function executar(mensagemCarregando, acao) {
    const pessoa = pessoaSelecionada();
    if (!pessoa) {
        return;
    }
    botaoCatalogo.disabled = botaoIa.disabled = true;
    resultado.innerHTML = `<p class="carregando">${mensagemCarregando}</p>`;
    try {
        resultado.innerHTML = await acao(pessoa);
    } catch (erro) {
        resultado.innerHTML = "";
        mostrarErro(erro);
    } finally {
        botaoCatalogo.disabled = botaoIa.disabled = false;
    }
}

function sugerirDoCatalogo() {
    return executar("Buscando no catálogo...", async (pessoa) => {
        const sugestoes = await api.sugestoesDoCatalogo(pessoa.id);
        if (sugestoes.length === 0) {
            return cabecalho(pessoa, "Sugestões do catálogo") + `
                <p class="vazio">Nenhum presente do catálogo atende a este perfil.
                Cadastre mais presentes ou experimente as ideias com IA.</p>`;
        }
        const cartoes = sugestoes.map(({ presente, motivos }, indice) => `
            <article class="cartao item sugestao">
                <div class="item__topo">
                    <span class="sugestao__posicao">${indice + 1}</span>
                    <div class="item__info">
                        <h3 class="item__titulo">${escaparHtml(presente.nome)}</h3>
                        <p class="item__subtitulo">${escaparHtml(descricao("categorias", presente.categoria))}</p>
                    </div>
                    <span class="item__preco">${formatarMoeda(presente.preco)}</span>
                </div>
                <p class="item__descricao">${etiquetas(motivos, "etiqueta--motivo")}</p>
            </article>`).join("");
        return cabecalho(pessoa, "Sugestões do catálogo") + `<div class="grade-sugestoes">${cartoes}</div>`;
    });
}

function sugerirComIa() {
    return executar("Consultando o Gemini, isso pode levar alguns segundos...", async (pessoa) => {
        const { ideias, descartadas } = await api.ideiasComIa(pessoa.id);
        const cartoes = ideias.map((ideia, indice) => `
            <article class="cartao item sugestao sugestao--ia">
                <div class="item__topo">
                    <span class="sugestao__posicao">${indice + 1}</span>
                    <div class="item__info">
                        <h3 class="item__titulo">${escaparHtml(ideia.nome)}</h3>
                        <p class="item__subtitulo">${escaparHtml(ideia.categoria)}</p>
                    </div>
                    <span class="item__preco">≈ ${formatarMoeda(ideia.precoEstimado)}</span>
                </div>
                <p class="item__descricao">${escaparHtml(ideia.motivo)}</p>
            </article>`).join("");

        const vazio = ideias.length === 0
            ? `<p class="vazio">A IA não retornou ideias dentro do orçamento e das restrições. Tente novamente.</p>`
            : "";
        const avisoDescartadas = descartadas > 0
            ? `${descartadas} ideia(s) da IA foram descartadas por estarem fora do orçamento
               ou relacionadas ao que a pessoa não gosta. `
            : "";
        return cabecalho(pessoa, "Ideias com IA") + vazio
            + `<div class="grade-sugestoes">${cartoes}</div>`
            + `<p class="nota">${avisoDescartadas}Preços estimados pela IA; confira antes de comprar.</p>`;
    });
}
