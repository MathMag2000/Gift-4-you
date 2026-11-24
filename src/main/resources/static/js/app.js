// Ponto de entrada do site: confere a sessão, monta o menu lateral e troca de tela conforme o endereço (#/...).
import { api, EVENTO_SESSAO_EXPIRADA } from "./api.js";
import { mostrarDeslogado, sair } from "./conta.js";
import { icones, logotipo } from "./componentes/icones.js";
import { carregarOpcoes } from "./opcoes.js";
import { telaCatalogo, telaFormularioPresente } from "./telas/catalogo.js";
import { telaComoFunciona } from "./telas/como-funciona.js";
import { telaConfiguracoes } from "./telas/configuracoes.js";
import { telaFavoritos } from "./telas/favoritos.js";
import { telaHistorico } from "./telas/historico.js";
import { telaInicio } from "./telas/inicio.js";
import { telaFormularioPessoa } from "./telas/pessoa-formulario.js";
import { telaDetalhesPessoa, telaPessoas } from "./telas/pessoas.js";
import { escaparHtml, marcadorErroImagem, mostrarErro } from "./util.js";

/** Itens do menu: os do wireframe (Início, Favoritos, Históricos, Configurações, Pessoas) e o Catálogo. */
const MENU = [
    { secao: "inicio", rotulo: "Início", icone: icones.inicio },
    { secao: "favoritos", rotulo: "Favoritos", icone: icones.coracao },
    { secao: "historico", rotulo: "Históricos", icone: icones.relogio },
    { secao: "configuracoes", rotulo: "Configurações", icone: icones.configuracoes },
    { secao: "pessoas", rotulo: "Pessoas", icone: icones.pessoa },
    { secao: "catalogo", rotulo: "Catálogo", icone: icones.presente },
];

/** Rotas: padrão do endereço, seção destacada no menu e a tela. */
const ROTAS = [
    { padrao: /^\/inicio$/, secao: "inicio", tela: telaInicio, titulo: "Início" },
    { padrao: /^\/como-funciona$/, secao: "inicio", tela: telaComoFunciona, titulo: "Como funciona" },
    { padrao: /^\/sugestoes\/nova$/, secao: "inicio", titulo: "Quero sugestões",
        tela: (principal) => telaFormularioPessoa(principal, { modo: "sugestoes" }) },
    { padrao: /^\/pessoas$/, secao: "pessoas", tela: telaPessoas, titulo: "Pessoas" },
    { padrao: /^\/pessoas\/nova$/, secao: "pessoas", titulo: "Cadastrar pessoa",
        tela: (principal) => telaFormularioPessoa(principal, { modo: "cadastrar" }) },
    { padrao: /^\/pessoas\/(\d+)$/, secao: "pessoas", titulo: "Pessoa",
        tela: (principal, [id], consulta) => telaDetalhesPessoa(principal,
            { id, gerarAutomaticamente: consulta.get("gerar") === "1" }) },
    { padrao: /^\/pessoas\/(\d+)\/editar$/, secao: "pessoas", titulo: "Editar pessoa",
        tela: (principal, [id]) => telaFormularioPessoa(principal, { modo: "editar", id }) },
    // Endereço antigo: as sugestões agora ficam na própria página da pessoa.
    { padrao: /^\/pessoas\/(\d+)\/sugestoes$/, secao: "pessoas", titulo: "Pessoa",
        tela: (principal, [id]) => location.replace(`#/pessoas/${id}`) },
    { padrao: /^\/favoritos$/, secao: "favoritos", titulo: "Favoritos",
        tela: (principal, _, consulta) => telaFavoritos(principal, { consulta }) },
    { padrao: /^\/historico$/, secao: "historico", titulo: "Histórico",
        tela: (principal, _, consulta) => telaHistorico(principal, { consulta }) },
    { padrao: /^\/catalogo$/, secao: "catalogo", tela: telaCatalogo, titulo: "Catálogo" },
    { padrao: /^\/catalogo\/novo$/, secao: "catalogo", titulo: "Cadastrar presente",
        tela: (principal, _, consulta) => telaFormularioPresente(principal, { pessoaInicial: consulta.get("pessoa") }) },
    { padrao: /^\/catalogo\/(\d+)\/editar$/, secao: "catalogo", titulo: "Editar presente",
        tela: (principal, [id]) => telaFormularioPresente(principal, { id }) },
    { padrao: /^\/configuracoes$/, secao: "configuracoes", tela: telaConfiguracoes, titulo: "Configurações" },
];

const principal = document.getElementById("principal");

function montarMenuLateral(conta) {
    document.getElementById("lateral").innerHTML = `
        ${logotipo()}
        <hr class="lateral__divisor">
        <nav class="menu" aria-label="Seções">
            ${MENU.map((item) => `
                <a class="menu__item" href="#/${item.secao}" data-secao="${item.secao}">
                    <span class="menu__icone">${item.icone()}</span>${item.rotulo}
                </a>`).join("")}
        </nav>
        <div class="lateral__rodape">
            <span class="lateral__usuario" title="${escaparHtml(conta.email)}">${escaparHtml(conta.nome)}</span>
            <button type="button" class="lateral__sair" data-acao="sair">${icones.sair()} Sair</button>
        </div>`;
    document.querySelector('#lateral [data-acao="sair"]').addEventListener("click", sair);
}

function destacarMenu(secao) {
    document.querySelectorAll(".menu__item").forEach((item) => {
        const ativo = item.dataset.secao === secao;
        item.classList.toggle("menu__item--ativo", ativo);
        if (ativo) {
            item.setAttribute("aria-current", "page");
        } else {
            item.removeAttribute("aria-current");
        }
    });
}

let navegacaoAtual = 0;

/** Desenha a tela do endereço atual num conteiner novo, para que eventos da tela anterior não fiquem para trás. */
async function navegar() {
    const [caminho, textoConsulta = ""] = location.hash.replace(/^#/, "").split("?");
    const rota = ROTAS.find((candidata) => candidata.padrao.test(caminho));
    if (!rota) {
        location.replace("#/inicio");
        return;
    }
    const numero = ++navegacaoAtual;
    destacarMenu(rota.secao);
    document.title = `${rota.titulo} - Presente Ideal`;

    const conteiner = document.createElement("div");
    conteiner.className = "tela";
    principal.replaceChildren(conteiner);
    principal.scrollTop = 0;
    try {
        await rota.tela(conteiner, rota.padrao.exec(caminho).slice(1), new URLSearchParams(textoConsulta));
    } catch (erro) {
        if (numero !== navegacaoAtual) {
            return;
        }
        if (erro.status === 404) {
            conteiner.innerHTML = `<p class="vazio">Não encontramos o que você procurava.
                <a class="link-discreto" href="#/inicio">Voltar ao início</a></p>`;
        }
        if (erro.status !== 401) {
            mostrarErro(erro);
        }
    }
}

/** Imagens de lojas podem sair do ar ou bloquear o acesso: troca pelo quadro com "X" do wireframe. */
function substituirImagensComErro() {
    document.addEventListener("error", (evento) => {
        const imagem = evento.target;
        if (imagem instanceof HTMLImageElement && imagem.classList.contains("imagem-produto")) {
            imagem.replaceWith(marcadorErroImagem(imagem.className));
        }
    }, true);
}

async function iniciar() {
    substituirImagensComErro();
    window.addEventListener(EVENTO_SESSAO_EXPIRADA,
        () => mostrarDeslogado("Sua sessão expirou. Entre novamente."), { once: true });

    let conta;
    try {
        conta = await api.contaAtual();
    } catch (erro) {
        mostrarDeslogado();
        mostrarErro(erro);
        return;
    }
    if (!conta) {
        mostrarDeslogado();
        return;
    }

    try {
        await carregarOpcoes();
    } catch (erro) {
        mostrarErro(erro);
        return;
    }
    montarMenuLateral(conta);
    document.body.dataset.estado = "logado";
    window.addEventListener("hashchange", navegar);
    await navegar();
}

iniciar();
