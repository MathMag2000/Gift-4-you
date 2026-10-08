import { api } from "./api.js";
import { renderizarItem } from "./itens.js";
import { nomeDaPessoa, preencherSeletorPessoas } from "./seletor-pessoas.js";
import { escaparHtml, formatarDataHora, mostrarAviso, mostrarErro } from "./util.js";

const filtroPessoa = document.getElementById("filtro-favoritos");
const lista = document.getElementById("lista-favoritos");
let pessoas = [];
let favoritos = [];

export function iniciarFavoritos() {
    filtroPessoa.addEventListener("change", () => carregarFavoritos().catch(mostrarErro));
    lista.addEventListener("click", removerFavorito);
}

/** Chamado ao abrir a aba, pois pessoas e favoritos podem ter mudado em outras abas. */
export async function atualizarFavoritos() {
    pessoas = await api.listarPessoas();
    preencherSeletorPessoas(filtroPessoa, pessoas, "Todas as pessoas");
    await carregarFavoritos();
}

async function carregarFavoritos() {
    favoritos = await api.listarFavoritos(filtroPessoa.value);
    renderizar();
}

function renderizar() {
    if (favoritos.length === 0) {
        lista.innerHTML = `<p class="vazio">Nenhum favorito ainda. Gere sugestões e clique em ☆ Favoritar.</p>`;
        return;
    }
    lista.innerHTML = favoritos.map((favorito) => renderizarItem(favorito.item, favorito.origem, {
        rodape: `<p class="nota">Para <strong>${escaparHtml(nomeDaPessoa(pessoas, favorito.pessoaId))}</strong>
                 · favoritado em ${formatarDataHora(favorito.favoritadoEm)}</p>`,
        acoes: `<button type="button" class="botao botao--perigo botao--pequeno" data-acao="remover"
                        data-id="${favorito.id}">Remover dos favoritos</button>`,
    })).join("");
}

async function removerFavorito(evento) {
    const botao = evento.target.closest('button[data-acao="remover"]');
    if (!botao) {
        return;
    }
    try {
        await api.removerFavorito(Number(botao.dataset.id));
        mostrarAviso("Removido dos favoritos.");
        await carregarFavoritos();
    } catch (erro) {
        mostrarErro(erro);
    }
}
