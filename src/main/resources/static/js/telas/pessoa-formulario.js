import { api } from "../api.js";
import {
    bloco, botaoVoltar, cabecalhoPagina, campo, tags, valorMarcado, valoresMarcados,
} from "../componentes/formulario.js";
import { icones } from "../componentes/icones.js";
import { listaDeOpcoes, valoresForaDasOpcoes } from "../opcoes.js";
import {
    comBotaoOcupado, escaparHtml, listaParaTexto, mostrarAviso, mostrarErro, numeroOuNulo, textoParaLista,
} from "../util.js";

const TAMANHO_MAXIMO_OBSERVACOES = 500;

/** Coisas que a pessoa pode não gostar, oferecidas como tags; o valor é o termo usado no filtro. */
const NAO_GOSTA = ["jogos", "livros", "perfumes", "roupas", "tecnologia", "esportes", "gastronomia"]
    .map((termo) => ({ valor: termo, descricao: `Não gosta de ${termo}` }));

/**
 * Três usos do mesmo formulário:
 * - "sugestoes": a partir do início; ao salvar, já mostra as sugestões;
 * - "cadastrar": a partir da lista de pessoas;
 * - "editar": altera uma pessoa existente.
 */
const MODOS = {
    sugestoes: {
        titulo: () => "Conte mais sobre a pessoa",
        subtitulo: () => "Quanto mais informações você fornecer, mais precisas serão nossas sugestões.",
        botao: `Encontrar presentes ${icones.seta()}`,
        voltar: () => "#/inicio",
        destino: (pessoa) => `#/pessoas/${pessoa.id}`,
        aviso: "Pessoa cadastrada. Buscando sugestões...",
    },
    cadastrar: {
        titulo: () => "Cadastrar nova pessoa",
        subtitulo: () => "Quanto mais informações você fornecer, mais precisas serão nossas sugestões.",
        botao: "Salvar pessoa",
        voltar: () => "#/pessoas",
        destino: (pessoa) => `#/pessoas/${pessoa.id}`,
        aviso: "Pessoa cadastrada.",
    },
    editar: {
        titulo: () => "Editar pessoa",
        subtitulo: (pessoa) => `Atualize as informações de <strong>${escaparHtml(pessoa.nome)}</strong>.`,
        botao: "Salvar alterações",
        voltar: (pessoa) => `#/pessoas/${pessoa.id}`,
        destino: (pessoa) => `#/pessoas/${pessoa.id}`,
        aviso: "Alterações salvas.",
    },
};

const PESSOA_VAZIA = {
    nome: "", idade: "", vinculo: null, gostos: [], naoGosta: [], estilos: [], ocasiao: null,
    orcamento: { maximo: "" }, observacoes: "",
};

export async function telaFormularioPessoa(principal, { modo, id }) {
    const config = MODOS[modo];
    const pessoa = modo === "editar" ? await api.buscarPessoa(id) : PESSOA_VAZIA;
    const termosNaoGosta = NAO_GOSTA.map((opcao) => opcao.valor);

    principal.innerHTML = `
        ${modo === "editar" ? botaoVoltar(config.voltar(pessoa), pessoa.nome) : ""}
        ${cabecalhoPagina(config.titulo(pessoa), config.subtitulo(pessoa))}
        <form class="formulario-largo" novalidate>
            ${bloco("Sobre a pessoa", `
                <div class="linha-campos">
                    ${campo("Nome da pessoa", `<input name="nome" type="text" maxlength="100" autocomplete="off"
                        placeholder="Ex: Maria" value="${escaparHtml(pessoa.nome)}">`, "campo--flexivel")}
                    ${campo("Idade", `<input name="idade" type="number" min="0" max="120" placeholder="Ex: 28"
                        value="${escaparHtml(pessoa.idade)}">`, "campo--idade")}
                </div>
                <div class="campo">
                    <span class="campo__rotulo">Vínculo</span>
                    ${tags("vinculo", listaDeOpcoes("vinculos"), [pessoa.vinculo], { tipo: "radio", rotulo: "Vínculo" })}
                </div>`)}

            ${bloco("Gostos e interesses", `
                ${tags("gostos", listaDeOpcoes("gostos"), pessoa.gostos, { rotulo: "Gostos e interesses" })}
                <input name="outrosGostos" type="text" aria-label="Outros interesses, separados por vírgula"
                       placeholder="Adicionar outro interesse… (separe por vírgula)"
                       value="${escaparHtml(listaParaTexto(valoresForaDasOpcoes("gostos", pessoa.gostos)))}">`,
                "Selecione tudo que combina com a pessoa.")}

            ${bloco("O que a pessoa NÃO gosta", `
                ${tags("naoGosta", NAO_GOSTA, pessoa.naoGosta, { rotulo: "O que a pessoa não gosta" })}
                <input name="outrosNaoGosta" type="text" aria-label="Outros itens a evitar, separados por vírgula"
                       placeholder="Adicionar outro item a evitar… (separe por vírgula)"
                       value="${escaparHtml(listaParaTexto(pessoa.naoGosta.filter((t) => !termosNaoGosta.includes(t))))}">`,
                "Essas informações serão usadas para evitar sugestões inadequadas.")}

            ${bloco("Estilo / preferências", `
                ${tags("estilos", listaDeOpcoes("estilos"), pessoa.estilos, { rotulo: "Estilo" })}
                <div class="bloco__divisao">
                    <h2 class="bloco__titulo">Ocasião</h2>
                    ${tags("ocasiao", listaDeOpcoes("ocasioes"), [pessoa.ocasiao], { tipo: "radio", rotulo: "Ocasião" })}
                </div>`)}

            ${bloco("Orçamento", `
                ${campo("Valor máximo", `<input name="orcamentoMaximo" type="text" inputmode="decimal"
                    placeholder="R$ 300,00" value="${escaparHtml(pessoa.orcamento.maximo)}">`, "campo--valor")}
                <p class="bloco__ajuda">Mostraremos sugestões até esse valor.</p>`)}

            ${bloco("Informações adicionais", `
                ${campo("Existe algo importante sobre essa pessoa que devemos saber?",
                    `<textarea name="observacoes" rows="4" maxlength="${TAMANHO_MAXIMO_OBSERVACOES}"
                        placeholder="Ex: ela acabou de se mudar e precisa de itens para casa...">${escaparHtml(pessoa.observacoes)}</textarea>`)}`)}

            <div class="acoes acoes--rodape">
                <button type="submit" class="botao botao--grande">${config.botao}</button>
                <a class="botao botao--contorno" href="${config.voltar(pessoa)}">${modo === "editar" ? "Cancelar" : `${icones.voltar()} Voltar`}</a>
            </div>
        </form>`;

    const formulario = principal.querySelector("form");
    formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const campos = formulario.elements;
        const dados = {
            nome: campos.nome.value,
            idade: numeroOuNulo(campos.idade.value),
            vinculo: valorMarcado(formulario, "vinculo"),
            gostos: [...valoresMarcados(formulario, "gostos"), ...textoParaLista(campos.outrosGostos.value)],
            naoGosta: [...valoresMarcados(formulario, "naoGosta"), ...textoParaLista(campos.outrosNaoGosta.value)],
            estilos: valoresMarcados(formulario, "estilos"),
            ocasiao: valorMarcado(formulario, "ocasiao"),
            orcamento: { maximo: numeroOuNulo(campos.orcamentoMaximo.value) },
            observacoes: campos.observacoes.value,
        };
        if (Number.isNaN(dados.orcamento.maximo)) {
            mostrarAviso("Informe o orçamento máximo como um valor, por exemplo 150,00.", "erro");
            return;
        }
        const botao = formulario.querySelector('button[type="submit"]');
        try {
            const salva = await comBotaoOcupado(botao, "Salvando...", () => modo === "editar"
                ? api.alterarPessoa(pessoa.id, dados)
                : api.cadastrarPessoa(dados));
            mostrarAviso(config.aviso);
            location.hash = config.destino(salva);
        } catch (erro) {
            mostrarErro(erro);
        }
    });
}
