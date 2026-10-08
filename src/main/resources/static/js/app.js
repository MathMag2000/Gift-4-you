import { atualizarFavoritos, iniciarFavoritos } from "./favoritos.js";
import { atualizarHistorico, iniciarHistorico } from "./historico.js";
import { carregarOpcoes, preencherSelects } from "./opcoes.js";
import { iniciarPessoas } from "./pessoas.js";
import { iniciarPresentes } from "./presentes.js";
import { atualizarPessoas, iniciarSugestoes } from "./sugestoes.js";
import { mostrarErro } from "./util.js";

const abas = document.querySelectorAll(".aba");

/** Seções que recarregam os dados ao serem abertas, pois dependem do que mudou em outras abas. */
const aoAbrir = {
    sugestoes: atualizarPessoas,
    favoritos: atualizarFavoritos,
    historico: atualizarHistorico,
};

function abrirSecao(nome) {
    abas.forEach((aba) => aba.classList.toggle("aba--ativa", aba.dataset.secao === nome));
    document.querySelectorAll(".secao").forEach((secao) => {
        secao.classList.toggle("secao--ativa", secao.id === `secao-${nome}`);
    });
    aoAbrir[nome]?.().catch(mostrarErro);
}

async function iniciar() {
    abas.forEach((aba) => aba.addEventListener("click", () => abrirSecao(aba.dataset.secao)));
    try {
        await carregarOpcoes();
        preencherSelects();
        iniciarSugestoes();
        iniciarFavoritos();
        iniciarHistorico();
        await Promise.all([iniciarPessoas(), iniciarPresentes()]);
    } catch (erro) {
        mostrarErro(erro);
    }
}

iniciar();
