import { api } from "../api.js";
import { EstadoFavoritos } from "../favoritar.js";
import { cabecalhoPagina, seletorPessoas, vazio } from "../componentes/formulario.js";
import { icones } from "../componentes/icones.js";
import { renderizarProdutos } from "../componentes/produtos.js";
import { descricao } from "../opcoes.js";
import { escaparHtml, formatarDataHora, formatarMoeda, mostrarErro } from "../util.js";

function resumo(registro) {
    const quantidade = registro.itens.length;
    return quantidade === 0 ? "nenhuma ideia encontrada"
        : quantidade === 1 ? "1 ideia" : `${quantidade} ideias`;
}

export async function telaHistorico(principal, { consulta }) {
    const pessoas = await api.listarPessoas();
    const pessoaInicial = consulta.get("pessoa") ?? "";

    principal.innerHTML = `
        ${cabecalhoPagina("Histórico", "Todas as ideias geradas com IA, das mais recentes para as mais antigas.",
            pessoas.length > 1 ? seletorPessoas("filtro-historico", pessoas, pessoaInicial) : "")}
        <div id="lista-historico" class="lista"></div>`;

    const lista = principal.querySelector("#lista-historico");
    const filtro = principal.querySelector("#filtro-historico");
    const favoritos = new EstadoFavoritos();
    let registros = [];

    async function carregar() {
        const pessoaId = (filtro ? filtro.value : pessoaInicial) || undefined;
        [registros] = await Promise.all([api.listarHistorico(pessoaId), favoritos.carregar()]);
        if (registros.length === 0) {
            lista.innerHTML = vazio(icones.relogio(32),
                "Nenhuma ideia gerada com IA ainda.<br>Abra uma pessoa e use o botão \"Gerar ideias com IA\".",
                `<a class="botao" href="#/pessoas">Ver pessoas ${icones.seta()}</a>`);
            return;
        }
        lista.innerHTML = registros.map((registro, indice) => `
            <article class="cartao registro ${registro.pessoaRemovida ? "registro--pessoa-removida" : ""}"
                     data-indice="${indice}">
                <div class="registro__topo">
                    <div>
                        <h2 class="registro__titulo">
                            ${registro.pessoaRemovida
                                ? escaparHtml(registro.pessoaNome)
                                : `<a class="registro__pessoa" href="#/pessoas/${registro.pessoaId}">${escaparHtml(registro.pessoaNome)}</a>`}
                            <span class="selo selo--ia">${icones.brilho(11)}${escaparHtml(descricao("origens", registro.origem))}</span>
                            ${registro.pessoaRemovida ? `<span class="selo selo--removida">Pessoa excluída</span>` : ""}
                        </h2>
                        <p class="registro__dados">
                            ${formatarDataHora(registro.realizadoEm)} · ${escaparHtml(descricao("ocasioes", registro.ocasiao))}
                            · até ${formatarMoeda(registro.orcamento.maximo)}
                        </p>
                    </div>
                    <span class="registro__resumo">${resumo(registro)}</span>
                </div>
                ${registro.itens.length ? `
                    <button type="button" class="botao botao--contorno botao--pequeno" data-acao="ver"
                            aria-expanded="false">Ver ideias</button>
                    <div class="grade-produtos registro__itens" hidden></div>` : ""}
            </article>`).join("");
    }

    lista.addEventListener("click", (evento) => {
        const botao = evento.target.closest('[data-acao="ver"]');
        if (!botao) {
            return;
        }
        const artigo = botao.closest(".registro");
        const registro = registros[Number(artigo.dataset.indice)];
        const grade = artigo.querySelector(".registro__itens");
        const abrir = grade.hidden;
        if (abrir) {
            renderizarProdutos(grade, registro.itens.map((item) => ({
                item, origem: registro.origem, pessoaId: registro.pessoaId,
            })), favoritos);
        }
        grade.hidden = !abrir;
        botao.setAttribute("aria-expanded", abrir);
        botao.textContent = abrir ? "Ocultar ideias" : "Ver ideias";
    });

    filtro?.addEventListener("change", () => carregar().catch(mostrarErro));
    await carregar();
}
