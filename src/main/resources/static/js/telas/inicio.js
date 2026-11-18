import { api } from "../api.js";
import { EstadoFavoritos } from "../favoritar.js";
import { CATALOGO } from "../itens.js";
import { icones, marcadorImagem } from "../componentes/icones.js";
import { nomeDaPessoa, vazio } from "../componentes/formulario.js";
import { renderizarProdutos } from "../componentes/produtos.js";

const LIMITE_SUGESTOES = 12;

/** Ilustração do topo: o quadro com "X" do wireframe e um presente desenhado em traço. */
const ILUSTRACAO = `
    <div class="destaque__ilustracao" aria-hidden="true">
        ${marcadorImagem("destaque__quadro")}
        <svg class="destaque__presente" viewBox="0 0 120 120" fill="none" stroke="currentColor" stroke-width="2.2"
             stroke-linecap="round" stroke-linejoin="round">
            <rect x="22" y="48" width="76" height="16" rx="2" fill="#fff"/>
            <path d="M28 64v38a4 4 0 004 4h56a4 4 0 004-4V64" fill="#fff"/>
            <path d="M60 48v58"/>
            <path d="M60 48c-6-14-26-20-26-8 0 6 14 8 26 8z"/>
            <path d="M60 48c6-14 26-20 26-8 0 6-14 8-26 8z"/>
            <path d="M8 30h8M12 26v8M104 20h8M108 16v8M106 70h8M110 66v8M10 86h6M13 83v6" stroke-width="1.6"/>
        </svg>
    </div>`;

/**
 * Sugestões mais recentes do histórico, sem repetições, para a seção "Sugestões para você".
 * Vêm do histórico porque cada sugestão pertence a uma pessoa, o que permite favoritá-la.
 */
function sugestoesRecentes(registros, pessoas) {
    const vistos = new Set();
    const entradas = [];
    for (const registro of registros) {
        for (const item of registro.itens) {
            const chave = `${registro.pessoaId}|${registro.origem}|${
                registro.origem === CATALOGO ? item.presenteId : item.nome.toLowerCase()}`;
            if (!vistos.has(chave)) {
                vistos.add(chave);
                entradas.push({ item, origem: registro.origem, pessoaId: registro.pessoaId,
                    nota: `Para ${nomeDaPessoa(pessoas, registro.pessoaId)}` });
            }
        }
    }
    return entradas.slice(0, LIMITE_SUGESTOES);
}

export async function telaInicio(principal) {
    principal.innerHTML = `
        <section class="cartao destaque">
            <div class="destaque__texto">
                <div>
                    <h1 class="destaque__titulo">Encontre o<br>presente perfeito</h1>
                    <p class="destaque__subtitulo">Sugestões personalizadas para cada pessoa e ocasião.</p>
                </div>
                <div class="destaque__acoes">
                    <a class="botao botao--grande" href="#/sugestoes/nova">Quero sugestões →</a>
                    <a class="botao botao--contorno botao--largo" href="#/como-funciona">Como funciona?</a>
                </div>
            </div>
            ${ILUSTRACAO}
        </section>

        <section class="secao-sugestoes">
            <div class="secao-sugestoes__cabecalho">
                <h2 class="titulo-secao">Sugestões para você</h2>
                <a class="link-discreto" href="#/historico">Ver todas →</a>
            </div>
            <div id="carrossel" class="carrossel"><p class="carregando">Carregando sugestões...</p></div>
        </section>`;

    const carrossel = principal.querySelector("#carrossel");
    const favoritos = new EstadoFavoritos();
    const [pessoas, registros] = await Promise.all([api.listarPessoas(), api.listarHistorico(), favoritos.carregar()]);
    const entradas = sugestoesRecentes(registros, pessoas);

    if (entradas.length === 0) {
        carrossel.innerHTML = vazio(icones.presente(32),
            "Ainda não há sugestões por aqui.<br>Conte sobre alguém especial e veja as ideias aparecerem.",
            `<a class="botao" href="#/sugestoes/nova">Quero sugestões →</a>`);
        return;
    }

    carrossel.innerHTML = `
        <div class="carrossel__faixa"></div>
        <button type="button" class="carrossel__seta" aria-label="Ver mais sugestões">${icones.direita()}</button>`;
    const faixa = carrossel.querySelector(".carrossel__faixa");
    renderizarProdutos(faixa, entradas, favoritos);

    const seta = carrossel.querySelector(".carrossel__seta");
    const atualizarSeta = () => {
        seta.hidden = faixa.scrollWidth <= faixa.clientWidth + 1;
    };
    seta.addEventListener("click", () => {
        const noFim = faixa.scrollLeft + faixa.clientWidth >= faixa.scrollWidth - 2;
        faixa.scrollTo({ left: noFim ? 0 : faixa.scrollLeft + faixa.clientWidth * 0.8, behavior: "smooth" });
    });
    new ResizeObserver(atualizarSeta).observe(faixa);
}
