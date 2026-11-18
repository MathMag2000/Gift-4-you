import { icones } from "../componentes/icones.js";
import { cabecalhoPagina } from "../componentes/formulario.js";

const PASSOS = [
    { titulo: "Conte para quem é",
        texto: "Informe algumas informações sobre a pessoa que receberá o presente." },
    { titulo: "Conte mais sobre a pessoa",
        texto: "Quanto mais detalhes você fornecer, como interesses, gostos, idade e ocasião, mais precisas serão as sugestões." },
    { titulo: "Receba sugestões",
        texto: "O sistema analisa as informações fornecidas e apresenta opções de presentes que combinam com o perfil da pessoa." },
];

export function telaComoFunciona(principal) {
    const passos = PASSOS.map((passo, indice) => `
        <li class="cartao passo">
            <span class="passo__numero">${indice + 1}</span>
            <p class="passo__titulo">${passo.titulo}</p>
            <p class="passo__texto">${passo.texto}</p>
        </li>
        ${indice < PASSOS.length - 1 ? `<li class="passo__seta" aria-hidden="true">${icones.direita(24)}</li>` : ""}`)
        .join("");

    principal.innerHTML = `
        ${cabecalhoPagina("Como funciona?",
            "O Presente Ideal ajuda você a encontrar presentes que combinam com cada pessoa e ocasião.")}
        <ol class="passos">${passos}</ol>
        <div class="dica">
            <p><strong>Dica: </strong>Quanto mais informações você fornecer, maior será a chance de encontrarmos
                o presente ideal.</p>
        </div>
        <div class="acoes">
            <a class="botao botao--grande" href="#/sugestoes/nova">Quero começar →</a>
            <a class="botao botao--contorno" href="#/inicio">← Voltar</a>
        </div>`;
}
