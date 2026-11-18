import { api } from "../api.js";
import {
    bloco, botaoVoltar, cabecalhoPagina, campo, tags, valoresMarcados, vazio,
} from "../componentes/formulario.js";
import { icones } from "../componentes/icones.js";
import { confirmar } from "../componentes/modal.js";
import { descricao, listaDeOpcoes } from "../opcoes.js";
import {
    comBotaoOcupado, enderecoParaExibir, escaparHtml, formatarMoeda, imagemProduto, listaParaTexto, mostrarAviso,
    mostrarErro, numeroOuNulo, textoOuNulo, textoParaLista, urlSegura,
} from "../util.js";

export async function telaCatalogo(principal) {
    const presentes = await api.listarPresentes();
    const lista = presentes.length === 0
        ? vazio(icones.presente(32), "Nenhum presente no catálogo ainda.<br>Os presentes cadastrados aqui entram nas sugestões.")
        : `<div class="lista">${presentes.map((presente) => {
            const link = urlSegura(presente.linkCompra);
            return `
            <article class="cartao linha-presente">
                ${imagemProduto(presente.imagemUrl, presente.nome, "linha-presente__foto")}
                <div class="linha-presente__info">
                    <div class="linha-presente__topo">
                        <div>
                            <h2 class="linha-presente__nome">${escaparHtml(presente.nome)}</h2>
                            <p class="linha-presente__dados">${escaparHtml(descricao("categorias", presente.categoria))}
                                · ${escaparHtml(presente.ocasioes.map((o) => descricao("ocasioes", o)).join(", "))}</p>
                        </div>
                        <span class="linha-presente__preco">${formatarMoeda(presente.preco)}</span>
                    </div>
                    ${presente.descricao ? `<p class="linha-presente__descricao">${escaparHtml(presente.descricao)}</p>` : ""}
                    <div class="acoes">
                        ${link ? `<a class="botao botao--contorno botao--pequeno" href="${escaparHtml(link)}" target="_blank"
                                     rel="noopener noreferrer">Ver na loja ${icones.externo()}</a>` : ""}
                        <a class="botao botao--contorno botao--pequeno" href="#/catalogo/${presente.id}/editar">Editar</a>
                        <button type="button" class="botao botao--remover botao--pequeno" data-acao="remover"
                                data-id="${presente.id}">Remover</button>
                    </div>
                </div>
            </article>`;
        }).join("")}</div>`;

    principal.innerHTML = `
        ${cabecalhoPagina("Catálogo de presentes",
            "Presentes usados nas sugestões do catálogo, compartilhados entre todas as contas.",
            `<a class="botao" href="#/catalogo/novo">+ Cadastrar presente</a>`)}
        ${lista}`;

    principal.addEventListener("click", async (evento) => {
        const botao = evento.target.closest('[data-acao="remover"]');
        if (!botao) {
            return;
        }
        const presente = presentes.find((item) => item.id === Number(botao.dataset.id));
        const confirmado = await confirmar({
            titulo: "Deseja realmente remover este presente?",
            texto: "Ele deixará de aparecer nas sugestões. Favoritos e histórico já salvos continuam iguais.",
        });
        if (!confirmado) {
            return;
        }
        try {
            await api.removerPresente(presente.id);
            mostrarAviso("Presente removido.");
            // Pede ao roteador (app.js) para desenhar a tela de novo, num contêiner novo.
            window.dispatchEvent(new Event("hashchange"));
        } catch (erro) {
            mostrarErro(erro);
        }
    });
}

const PRESENTE_VAZIO = {
    nome: "", categoria: null, descricao: "", preco: "", caracteristicas: [], ocasioes: [], linkCompra: "",
};

export async function telaFormularioPresente(principal, { id }) {
    const editando = id !== undefined;
    const presente = editando ? await api.buscarPresente(id) : PRESENTE_VAZIO;

    principal.innerHTML = `
        ${botaoVoltar("#/catalogo", "Catálogo")}
        ${cabecalhoPagina(editando ? "Editar presente" : "Cadastrar presente",
            "A foto do produto é buscada automaticamente na página do link de compra.")}
        <form class="formulario-largo" novalidate>
            ${bloco("Sobre o presente", `
                <div class="linha-campos">
                    ${campo("Nome", `<input name="nome" type="text" maxlength="100" autocomplete="off"
                        placeholder="Ex: Fone Bluetooth" value="${escaparHtml(presente.nome)}">`, "campo--flexivel")}
                    ${campo("Preço (R$)", `<input name="preco" type="text" inputmode="decimal" placeholder="R$ 150,00"
                        value="${escaparHtml(presente.preco)}">`, "campo--valor")}
                </div>
                <div class="campo">
                    <span class="campo__rotulo">Categoria</span>
                    ${tags("categoria", listaDeOpcoes("categorias"), [presente.categoria], { tipo: "radio", rotulo: "Categoria" })}
                </div>
                ${campo("Descrição <small>opcional</small>",
                    `<textarea name="descricao" rows="2">${escaparHtml(presente.descricao)}</textarea>`)}`)}

            ${bloco("Loja", `
                ${campo("Link para compra", `<input name="linkCompra" type="url" autocomplete="off"
                    placeholder="https://www.loja.com.br/produto..." value="${escaparHtml(presente.linkCompra)}">`)}
                ${presente.linkCompra ? `<p class="bloco__ajuda">Atual: ${escaparHtml(enderecoParaExibir(presente.linkCompra))}</p>` : ""}`)}

            ${bloco("Características e ocasiões", `
                ${campo("Características <small>separadas por vírgula</small>", `<input name="caracteristicas" type="text"
                    placeholder="ex.: música, tecnologia, sem fio" value="${escaparHtml(listaParaTexto(presente.caracteristicas))}">`)}
                <div class="campo">
                    <span class="campo__rotulo">Ocasiões compatíveis</span>
                    ${tags("ocasioes", listaDeOpcoes("ocasioes"), presente.ocasioes, { rotulo: "Ocasiões compatíveis" })}
                </div>`)}

            <div class="acoes acoes--rodape">
                <button type="submit" class="botao botao--grande">${editando ? "Salvar alterações" : "Cadastrar presente"}</button>
                <a class="botao botao--contorno" href="#/catalogo">Cancelar</a>
            </div>
        </form>`;

    const formulario = principal.querySelector("form");
    formulario.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const campos = formulario.elements;
        const dados = {
            nome: campos.nome.value,
            categoria: textoOuNulo(formulario.querySelector('input[name="categoria"]:checked')?.value ?? ""),
            descricao: campos.descricao.value,
            preco: numeroOuNulo(campos.preco.value),
            caracteristicas: textoParaLista(campos.caracteristicas.value),
            ocasioes: valoresMarcados(formulario, "ocasioes"),
            linkCompra: campos.linkCompra.value,
        };
        if (Number.isNaN(dados.preco)) {
            mostrarAviso("Informe o preço como um valor, por exemplo 150,00.", "erro");
            return;
        }
        const botao = formulario.querySelector('button[type="submit"]');
        try {
            const salvo = await comBotaoOcupado(botao, "Salvando e buscando a foto...", () => editando
                ? api.alterarPresente(presente.id, dados)
                : api.cadastrarPresente(dados));
            const acao = editando ? "Presente alterado" : "Presente cadastrado";
            mostrarAviso(salvo.imagemUrl ? `${acao}.` : `${acao}, mas a loja não permitiu ler a foto do produto.`);
            location.hash = "#/catalogo";
        } catch (erro) {
            mostrarErro(erro);
        }
    });
}
