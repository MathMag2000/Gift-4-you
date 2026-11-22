import { api } from "../api.js";
import { EstadoFavoritos } from "../favoritar.js";
import { CATALOGO, IA, itemDeIdeia, itemDeSugestao } from "../itens.js";
import { icones } from "../componentes/icones.js";
import { confirmar } from "../componentes/modal.js";
import { botaoVoltar, cabecalhoPagina, vazio } from "../componentes/formulario.js";
import { renderizarProdutos } from "../componentes/produtos.js";
import { descricao } from "../opcoes.js";
import {
    capitalizar, comBotaoOcupado, escaparHtml, formatarDataHora, formatarMoeda, listaParaTexto, mostrarAviso,
    mostrarErro,
} from "../util.js";

export async function telaPessoas(principal) {
    const pessoas = await api.listarPessoas();
    const lista = pessoas.length === 0
        ? vazio(icones.pessoa(32), "Nenhuma pessoa cadastrada ainda.<br>Cadastre alguém para receber sugestões.")
        : `<div class="lista">${pessoas.map((pessoa) => `
            <a class="cartao linha-pessoa" href="#/pessoas/${pessoa.id}">
                <div class="linha-pessoa__info">
                    <span class="linha-pessoa__nome">${escaparHtml(pessoa.nome)}</span>
                    <span class="linha-pessoa__dados">
                        ${pessoa.idade} anos · ${escaparHtml(descricao("vinculos", pessoa.vinculo))}
                    </span>
                    ${pessoa.gostos.length
                        ? `<span class="linha-pessoa__gostos">${escaparHtml(capitalizar(listaParaTexto(pessoa.gostos)))}</span>`
                        : ""}
                </div>
                <span class="linha-pessoa__seta">${icones.direita()}</span>
            </a>`).join("")}</div>`;

    principal.innerHTML = `
        ${cabecalhoPagina("Pessoas", "Gerencie as pessoas e encontre sugestões personalizadas para cada uma.",
            `<a class="botao" href="#/pessoas/nova">${icones.mais()} Cadastrar nova pessoa</a>`)}
        ${lista}`;
}

function linhaDetalhe(rotulo, valor) {
    return `<div class="detalhe"><dt>${rotulo}</dt><dd>${valor || "—"}</dd></div>`;
}

function textoLista(lista) {
    return escaparHtml(capitalizar(listaParaTexto(lista)));
}

/**
 * Sem gostos, estilos nem observações, só restam vínculo, idade, ocasião e orçamento: pouco para
 * encontrar presentes que combinem com a pessoa.
 */
function temPoucasInformacoes(pessoa) {
    return pessoa.gostos.length === 0 && pessoa.estilos.length === 0 && !pessoa.observacoes?.trim();
}

/**
 * Página da pessoa: dados cadastrados, presentes cadastrados para ela, presentes do catálogo que combinam
 * com o perfil e as últimas ideias geradas com IA. Abrir a página não gera registro no histórico; só uma
 * nova geração de ideias com IA gera.
 */
export async function telaDetalhesPessoa(principal, { id }) {
    const pessoa = await api.buscarPessoa(id);
    const nome = escaparHtml(pessoa.nome);
    principal.innerHTML = `
        ${botaoVoltar("#/pessoas", "Pessoas")}
        ${cabecalhoPagina(nome, "", `
            <div class="acoes">
                <a class="botao botao--contorno-forte" href="#/pessoas/${pessoa.id}/editar">Editar pessoa</a>
                <button type="button" class="botao botao--remover" data-acao="remover">Remover pessoa</button>
            </div>`)}
        <dl class="cartao detalhes">
            ${linhaDetalhe("Idade", `${pessoa.idade} anos`)}
            ${linhaDetalhe("Vínculo", escaparHtml(descricao("vinculos", pessoa.vinculo)))}
            ${linhaDetalhe("Gostos e interesses", textoLista(pessoa.gostos))}
            ${linhaDetalhe("Não gosta", textoLista(pessoa.naoGosta))}
            ${linhaDetalhe("Estilo / preferências", textoLista(pessoa.estilos))}
            ${linhaDetalhe("Ocasião", escaparHtml(descricao("ocasioes", pessoa.ocasiao)))}
            ${linhaDetalhe("Orçamento", `até ${formatarMoeda(pessoa.orcamento.maximo)}`)}
            ${linhaDetalhe("Informações adicionais", escaparHtml(pessoa.observacoes))}
        </dl>

        <div id="aviso-perfil"></div>

        <section class="secao-sugestoes">
            <div class="secao-sugestoes__cabecalho">
                <div>
                    <h2 class="titulo-secao">Presentes cadastrados para ${nome}</h2>
                    <p class="nota">Presentes que você escolheu e cadastrou no catálogo para esta pessoa.</p>
                </div>
                <a class="botao botao--contorno botao--pequeno" href="#/catalogo/novo?pessoa=${pessoa.id}">
                    ${icones.mais(14)} Cadastrar presente</a>
            </div>
            <div id="presentes-pessoa"><p class="carregando">Carregando presentes...</p></div>
        </section>

        <section class="secao-sugestoes" id="secao-catalogo" hidden>
            <div>
                <h2 class="titulo-secao">Do catálogo</h2>
                <p class="nota">Outros presentes do seu catálogo que combinam com o perfil de ${nome}.</p>
            </div>
            <div id="sugestoes-catalogo"></div>
        </section>

        <section class="cartao painel-ia">
            <div class="painel-ia__cabecalho">
                <div class="painel-ia__texto">
                    <h2 class="titulo-secao"><span class="painel-ia__icone">${icones.brilho(17)}</span>Ideias com IA</h2>
                    <p class="nota">A IA sugere presentes fora do catálogo e procura cada um na Amazon.
                        Novas gerações não repetem ideias já sugeridas e ficam salvas no histórico.</p>
                    <p class="painel-ia__data" id="data-ia" hidden></p>
                </div>
                <button type="button" class="botao" id="botao-ia">${icones.brilho(16)} Gerar ideias com IA</button>
            </div>
            <div id="sugestoes-ia"><p class="carregando">Carregando ideias anteriores...</p></div>
        </section>`;

    principal.querySelector('[data-acao="remover"]').addEventListener("click", () => removerPessoa(pessoa));

    const favoritos = new EstadoFavoritos();
    const areaPresentes = principal.querySelector("#presentes-pessoa");
    const secaoCatalogo = principal.querySelector("#secao-catalogo");
    const areaCatalogo = principal.querySelector("#sugestoes-catalogo");
    const areaIa = principal.querySelector("#sugestoes-ia");
    const dataIa = principal.querySelector("#data-ia");
    const botaoIa = principal.querySelector("#botao-ia");

    function grade(area, itens, origem) {
        area.innerHTML = `<div class="grade-produtos"></div>`;
        renderizarProdutos(area.firstElementChild,
            itens.map((item) => ({ item, origem, pessoaId: pessoa.id })), favoritos);
    }

    function mostrarIdeias(itens, realizadoEm) {
        grade(areaIa, itens, IA);
        areaIa.insertAdjacentHTML("beforeend",
            `<p class="nota">Preços com ≈ são estimados pela IA; confira antes de comprar.</p>`);
        dataIa.textContent = realizadoEm ? `Últimas ideias geradas em ${formatarDataHora(realizadoEm)}.` : "";
        dataIa.hidden = !realizadoEm;
        botaoIa.innerHTML = `${icones.brilho(16)} Gerar novas ideias`;
    }

    const [presentes, sugestoes, registros] = await Promise.all([
        api.presentesDaPessoa(pessoa.id),
        api.sugestoesDoCatalogo(pessoa.id),
        api.listarHistorico(pessoa.id),
        favoritos.carregar(pessoa.id),
    ]);
    const ultimoRegistro = registros[0];

    if (presentes.length) {
        grade(areaPresentes, presentes.map(itemDeSugestao), CATALOGO);
    } else {
        areaPresentes.innerHTML = `<p class="nota nota--vazia">Nenhum presente cadastrado especificamente para ${nome}.</p>`;
    }

    if (sugestoes.length) {
        secaoCatalogo.hidden = false;
        grade(areaCatalogo, sugestoes.map(itemDeSugestao), CATALOGO);
    }

    if (ultimoRegistro) {
        mostrarIdeias(ultimoRegistro.itens, ultimoRegistro.realizadoEm);
    } else {
        areaIa.innerHTML = `<p class="nota nota--vazia">Nenhuma ideia gerada para ${nome} ainda.</p>`;
    }

    // O aviso só aparece quando realmente falta o que mostrar e o perfil tem informações de menos.
    if (temPoucasInformacoes(pessoa) && !presentes.length && !sugestoes.length && !ultimoRegistro) {
        principal.querySelector("#aviso-perfil").innerHTML = `
            <div class="dica">
                <p><strong>Poucas informações sobre ${nome}.</strong> Ainda não dá para sugerir presentes relevantes.
                   Adicione gostos, interesses ou estilo para receber sugestões melhores.
                   <a class="link-discreto" href="#/pessoas/${pessoa.id}/editar">Completar perfil</a></p>
            </div>`;
    }

    botaoIa.addEventListener("click", async () => {
        const conteudoAnterior = areaIa.innerHTML;
        areaIa.innerHTML = `<p class="carregando">Gerando ideias com IA, isso pode levar alguns segundos...</p>`;
        try {
            const { ideias, descartadas, repetidas } = await comBotaoOcupado(botaoIa, "Gerando...",
                () => api.ideiasComIa(pessoa.id));
            const avisoDescartadas = [
                descartadas > 0 ? `${descartadas} ideia(s) foram descartadas por estarem fora do orçamento ou `
                    + "relacionadas ao que a pessoa não gosta." : "",
                repetidas > 0 ? `${repetidas} ideia(s) já sugeridas antes foram deixadas de fora.` : "",
            ].filter(Boolean).join(" ");
            if (ideias.length === 0) {
                areaIa.innerHTML = conteudoAnterior;
                mostrarAviso(repetidas > 0 && descartadas === 0
                    ? "A IA só encontrou ideias já sugeridas antes para esta pessoa. Tente novamente."
                    : `A IA não retornou ideias novas dentro do orçamento e das restrições. ${avisoDescartadas} Tente novamente.`,
                    "erro");
                return;
            }
            mostrarIdeias(ideias.map(itemDeIdeia), new Date().toISOString());
            if (avisoDescartadas) {
                areaIa.insertAdjacentHTML("beforeend", `<p class="nota">${avisoDescartadas}</p>`);
            }
            mostrarAviso("Novas ideias geradas e salvas no histórico.");
        } catch (erro) {
            areaIa.innerHTML = conteudoAnterior;
            mostrarErro(erro);
        }
    });
}

async function removerPessoa(pessoa) {
    const confirmado = await confirmar({
        titulo: "Deseja realmente remover esta pessoa?",
        texto: "Esta ação não poderá ser desfeita. Os favoritos dela serão apagados; as ideias já geradas "
            + "continuam no histórico, indicando que a pessoa foi excluída.",
    });
    if (!confirmado) {
        return;
    }
    try {
        await api.removerPessoa(pessoa.id);
        mostrarAviso(`${pessoa.nome} foi removida.`);
        location.hash = "#/pessoas";
    } catch (erro) {
        mostrarErro(erro);
    }
}
