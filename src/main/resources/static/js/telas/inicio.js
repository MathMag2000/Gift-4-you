import { api } from "../api.js";
import { EstadoFavoritos } from "../favoritar.js";
import { CATALOGO } from "../itens.js";
import { icones } from "../componentes/icones.js";
import { vazio } from "../componentes/formulario.js";
import { renderizarProdutos } from "../componentes/produtos.js";

const LIMITE_SUGESTOES = 12;

/**
 * Ilustração do topo: painel verde com formas suaves e um presente em destaque no canto.
 * As cores vêm do CSS (classes ilustracao__*), pois a política de segurança não permite estilos embutidos.
 */
const ILUSTRACAO = `
    <div class="destaque__ilustracao" aria-hidden="true">
        <svg class="destaque__quadro" viewBox="0 0 360 240" preserveAspectRatio="xMidYMid slice">
            <rect class="ilustracao__fundo" width="360" height="240"/>
            <circle class="ilustracao__forma" cx="300" cy="36" r="92"/>
            <circle class="ilustracao__forma ilustracao__forma--clara" cx="40" cy="220" r="70"/>
            <circle class="ilustracao__anel" cx="300" cy="36" r="122"/>
            <g class="ilustracao__caixa">
                <rect x="126" y="92" width="108" height="24" rx="5"/>
                <path d="M134 116v58a7 7 0 007 7h78a7 7 0 007-7v-58"/>
            </g>
            <g class="ilustracao__fita">
                <rect x="172" y="92" width="16" height="89"/>
                <path d="M180 92c-9-20-38-28-38-11 0 9 20 11 38 11z"/>
                <path d="M180 92c9-20 38-28 38-11 0 9-20 11-38 11z"/>
            </g>
            <g class="ilustracao__brilho">
                <path d="M86 58l3.5 9 9 3.5-9 3.5-3.5 9-3.5-9-9-3.5 9-3.5z"/>
                <path d="M282 150l2.5 6.5 6.5 2.5-6.5 2.5-2.5 6.5-2.5-6.5-6.5-2.5 6.5-2.5z"/>
                <circle cx="256" cy="72" r="3"/>
                <circle cx="104" cy="160" r="2.5"/>
            </g>
        </svg>
        <span class="destaque__presente">${icones.coracao(44, true)}</span>
    </div>`;

/**
 * Ideias mais recentes do histórico, sem repetições, para a seção "Sugestões para você".
 * Vêm do histórico porque cada ideia pertence a uma pessoa, o que permite favoritá-la
 * (ideias de pessoas excluídas aparecem, mas sem o coração).
 */
function sugestoesRecentes(registros) {
    const vistos = new Set();
    const entradas = [];
    for (const registro of registros) {
        for (const item of registro.itens) {
            const chave = `${registro.pessoaId}|${registro.origem}|${
                registro.origem === CATALOGO ? item.presenteId : item.nome.toLowerCase()}`;
            if (!vistos.has(chave)) {
                vistos.add(chave);
                entradas.push({ item, origem: registro.origem, pessoaId: registro.pessoaId,
                    nota: `Para ${registro.pessoaNome}${registro.pessoaRemovida ? " (pessoa excluída)" : ""}` });
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
                    <h1 class="destaque__titulo">Encontre o<br><em>presente perfeito</em></h1>
                    <p class="destaque__subtitulo">Sugestões personalizadas para cada pessoa e ocasião.</p>
                </div>
                <div class="destaque__acoes">
                    <a class="botao botao--grande" href="#/sugestoes/nova">Quero sugestões ${icones.seta(18)}</a>
                    <a class="botao botao--contorno botao--largo" href="#/como-funciona">Como funciona?</a>
                </div>
            </div>
            ${ILUSTRACAO}
        </section>

        <section class="secao-sugestoes">
            <div class="secao-sugestoes__cabecalho">
                <h2 class="titulo-secao">Sugestões para você</h2>
                <a class="link-discreto" href="#/historico">Ver todas ${icones.seta(14)}</a>
            </div>
            <div id="carrossel" class="carrossel"><p class="carregando">Carregando sugestões...</p></div>
        </section>`;

    const carrossel = principal.querySelector("#carrossel");
    const favoritos = new EstadoFavoritos();
    const [registros] = await Promise.all([api.listarHistorico(), favoritos.carregar()]);
    const entradas = sugestoesRecentes(registros);

    if (entradas.length === 0) {
        carrossel.innerHTML = vazio(icones.presente(32),
            "Ainda não há sugestões por aqui.<br>Conte sobre alguém especial e veja as ideias aparecerem.",
            `<a class="botao" href="#/sugestoes/nova">Quero sugestões ${icones.seta()}</a>`);
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
