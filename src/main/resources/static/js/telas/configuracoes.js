import { api } from "../api.js";
import { sair } from "../conta.js";
import { cabecalhoPagina } from "../componentes/formulario.js";
import { icones } from "../componentes/icones.js";
import { escaparHtml } from "../util.js";

export async function telaConfiguracoes(principal) {
    const conta = await api.contaAtual();
    principal.innerHTML = `
        ${cabecalhoPagina("Configurações", "Dados da sua conta.")}
        <dl class="cartao detalhes">
            <div class="detalhe"><dt>Nome</dt><dd>${escaparHtml(conta?.nome)}</dd></div>
            <div class="detalhe"><dt>E-mail</dt><dd>${escaparHtml(conta?.email)}</dd></div>
        </dl>
        <div class="dica">
            <p>As pessoas, o catálogo de presentes, os favoritos e o histórico são visíveis somente para a sua conta.</p>
        </div>
        <div class="acoes">
            <button type="button" class="botao botao--contorno-forte" data-acao="sair">${icones.sair()} Sair da conta</button>
            <a class="botao botao--contorno" href="#/como-funciona">Como funciona?</a>
        </div>`;

    principal.querySelector('[data-acao="sair"]').addEventListener("click", sair);
}
