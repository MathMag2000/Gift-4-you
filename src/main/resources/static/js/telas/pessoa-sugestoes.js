import { api } from "../api.js";
import { EstadoFavoritos } from "../favoritar.js";
import { CATALOGO, IA, itemDeIdeia, itemDeSugestao } from "../itens.js";
import { botaoVoltar, vazio } from "../componentes/formulario.js";
import { icones } from "../componentes/icones.js";
import { renderizarProdutos } from "../componentes/produtos.js";
import { descricao } from "../opcoes.js";
import { comBotaoOcupado, escaparHtml, formatarMoeda, mostrarErro } from "../util.js";

export async function telaSugestoesPessoa(principal, { id }) {
    const pessoa = await api.buscarPessoa(id);
    const nome = escaparHtml(pessoa.nome);
    principal.innerHTML = `
        ${botaoVoltar(`#/pessoas/${pessoa.id}`, `Voltar para ${pessoa.nome}`)}
        <header class="cabecalho-pagina">
            <div>
                <h1 class="titulo-pagina titulo-pagina--menor">Sugestões para ${nome}</h1>
                <p class="subtitulo-pagina">Com base nas informações cadastradas, encontramos estas sugestões.</p>
                <p class="resumo-perfil">${escaparHtml(descricao("ocasioes", pessoa.ocasiao))}
                    · até ${formatarMoeda(pessoa.orcamento.maximo)}</p>
            </div>
        </header>

        <section class="secao-sugestoes">
            <h2 class="titulo-secao">Do catálogo</h2>
            <div id="sugestoes-catalogo"><p class="carregando">Buscando no catálogo...</p></div>
        </section>

        <section class="secao-sugestoes">
            <div class="secao-sugestoes__cabecalho">
                <h2 class="titulo-secao">Ideias com IA</h2>
                <button type="button" class="botao botao--contorno-forte" id="botao-ia">Gerar ideias com IA</button>
            </div>
            <div id="sugestoes-ia">
                <p class="nota">A IA sugere presentes fora do catálogo e procura cada um na Amazon.</p>
            </div>
        </section>

        <div class="dica">
            <p>Essas sugestões podem melhorar conforme você adicionar mais informações sobre a pessoa.
               <a class="link-discreto" href="#/pessoas/${pessoa.id}/editar">Editar ${nome}</a></p>
        </div>`;

    const favoritos = new EstadoFavoritos();
    const areaCatalogo = principal.querySelector("#sugestoes-catalogo");
    const areaIa = principal.querySelector("#sugestoes-ia");

    function grade(area, itens, origem) {
        area.innerHTML = `<div class="grade-produtos"></div>`;
        renderizarProdutos(area.firstElementChild,
            itens.map((item) => ({ item, origem, pessoaId: pessoa.id })), favoritos);
    }

    try {
        const [sugestoes] = await Promise.all([api.sugestoesDoCatalogo(pessoa.id), favoritos.carregar(pessoa.id)]);
        if (sugestoes.length === 0) {
            areaCatalogo.innerHTML = vazio(icones.presente(28),
                "Nenhum presente do catálogo combina com este perfil.<br>Experimente as ideias com IA ou cadastre mais presentes.",
                `<a class="link-discreto" href="#/catalogo/novo">Cadastrar presente no catálogo →</a>`);
        } else {
            grade(areaCatalogo, sugestoes.map(itemDeSugestao), CATALOGO);
        }
    } catch (erro) {
        areaCatalogo.innerHTML = "";
        mostrarErro(erro);
    }

    const botaoIa = principal.querySelector("#botao-ia");
    botaoIa.addEventListener("click", async () => {
        areaIa.innerHTML = `<p class="carregando">Gerando ideias com IA, isso pode levar alguns segundos...</p>`;
        try {
            const { ideias, descartadas } = await comBotaoOcupado(botaoIa, "Gerando...",
                () => api.ideiasComIa(pessoa.id));
            const avisoDescartadas = descartadas > 0
                ? `${descartadas} ideia(s) foram descartadas por estarem fora do orçamento ou relacionadas ao que a pessoa não gosta. `
                : "";
            if (ideias.length === 0) {
                areaIa.innerHTML = vazio(icones.presente(28),
                    "A IA não retornou ideias dentro do orçamento e das restrições. Tente novamente.");
            } else {
                grade(areaIa, ideias.map(itemDeIdeia), IA);
            }
            areaIa.insertAdjacentHTML("beforeend",
                `<p class="nota">${avisoDescartadas}Preços com ≈ são estimados pela IA; confira antes de comprar.</p>`);
            botaoIa.textContent = "Gerar novas ideias";
        } catch (erro) {
            areaIa.innerHTML = `<p class="nota">${escaparHtml(erro.message)}</p>`;
            mostrarErro(erro);
        }
    });
}
