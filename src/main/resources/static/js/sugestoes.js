import { api } from "./api.js";
import { atualizarBotaoFavoritar, EstadoFavoritos } from "./favoritar.js";
import { botaoFavoritar, CATALOGO, IA, itemDeIdeia, itemDeSugestao, renderizarItem } from "./itens.js";
import { descricao } from "./opcoes.js";
import { preencherSeletorPessoas } from "./seletor-pessoas.js";
import { escaparHtml, formatarMoeda, mostrarAviso, mostrarErro } from "./util.js";

const seletorPessoa = document.getElementById("pessoa-sugestao");
const resultado = document.getElementById("resultado-sugestoes");
const botaoCatalogo = document.getElementById("botao-sugestoes-catalogo");
const botaoIa = document.getElementById("botao-ideias-ia");
const favoritos = new EstadoFavoritos();
let pessoas = [];
let exibidos = { pessoaId: null, origem: null, itens: [] };

export function iniciarSugestoes() {
    botaoCatalogo.addEventListener("click", sugerirDoCatalogo);
    botaoIa.addEventListener("click", sugerirComIa);
    resultado.addEventListener("click", favoritarItem);
}

/** Recarrega as pessoas, pois podem ter sido alteradas na aba Pessoas. */
export async function atualizarPessoas() {
    pessoas = await api.listarPessoas();
    preencherSeletorPessoas(seletorPessoa, pessoas);
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

function renderizarItens(pessoa, origem, itens) {
    exibidos = { pessoaId: pessoa.id, origem, itens };
    const cartoes = itens.map((item, indice) => renderizarItem(item, origem, {
        posicao: indice + 1,
        acoes: botaoFavoritar(indice, favoritos.estaFavoritado(pessoa.id, origem, item)),
    })).join("");
    return `<div class="grade-sugestoes">${cartoes}</div>`;
}

async function executar(mensagemCarregando, acao) {
    const pessoa = pessoaSelecionada();
    if (!pessoa) {
        return;
    }
    botaoCatalogo.disabled = botaoIa.disabled = true;
    resultado.innerHTML = `<p class="carregando">${mensagemCarregando}</p>`;
    try {
        await favoritos.carregar(pessoa.id);
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
        const itens = (await api.sugestoesDoCatalogo(pessoa.id)).map(itemDeSugestao);
        if (itens.length === 0) {
            return cabecalho(pessoa, "Sugestões do catálogo") + `
                <p class="vazio">Nenhum presente do catálogo atende a este perfil.
                Cadastre mais presentes ou experimente as ideias com IA.</p>`;
        }
        return cabecalho(pessoa, "Sugestões do catálogo") + renderizarItens(pessoa, CATALOGO, itens);
    });
}

function sugerirComIa() {
    return executar("Consultando o Gemini, isso pode levar alguns segundos...", async (pessoa) => {
        const { ideias, descartadas } = await api.ideiasComIa(pessoa.id);
        const vazio = ideias.length === 0
            ? `<p class="vazio">A IA não retornou ideias dentro do orçamento e das restrições. Tente novamente.</p>`
            : "";
        const avisoDescartadas = descartadas > 0
            ? `${descartadas} ideia(s) da IA foram descartadas por estarem fora do orçamento
               ou relacionadas ao que a pessoa não gosta. `
            : "";
        return cabecalho(pessoa, "Ideias com IA") + vazio
            + renderizarItens(pessoa, IA, ideias.map(itemDeIdeia))
            + `<p class="nota">${avisoDescartadas}Preços estimados pela IA; confira antes de comprar.</p>`;
    });
}

async function favoritarItem(evento) {
    const botao = evento.target.closest('button[data-acao="favoritar"]');
    if (!botao) {
        return;
    }
    const item = exibidos.itens[Number(botao.dataset.indice)];
    botao.disabled = true;
    try {
        const favoritado = await favoritos.alternar(exibidos.pessoaId, exibidos.origem, item);
        atualizarBotaoFavoritar(botao, favoritado);
        mostrarAviso(favoritado ? "Adicionado aos favoritos." : "Removido dos favoritos.");
    } catch (erro) {
        mostrarErro(erro);
    } finally {
        botao.disabled = false;
    }
}
