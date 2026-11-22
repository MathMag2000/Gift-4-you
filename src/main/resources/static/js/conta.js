// Tela de login e de criação de conta, no estilo do wireframe.
import { api } from "./api.js";
import { icones, logotipo } from "./componentes/icones.js";
import { mostrarAviso, mostrarErro } from "./util.js";

const FORMATO_EMAIL = /^[^\s@]+@[^\s@.]+(\.[^\s@.]+)+$/;

const MODOS = {
    login: {
        titulo: "Entrar",
        subtitulo: "Acesse sua conta para continuar.",
        botao: `Entrar ${icones.seta()}`,
        links: `<button type="button" class="link-conta" data-modo="cadastro">
                    Ainda não tenho conta — <strong>Criar conta</strong></button>`,
    },
    cadastro: {
        titulo: "Criar conta",
        subtitulo: "Preencha os dados para criar sua conta.",
        botao: `Criar conta ${icones.seta()}`,
        links: `<button type="button" class="link-conta" data-modo="login">${icones.voltar(14)} Já tenho uma conta</button>`,
    },
};

function campo(rotulo, entrada) {
    return `<label class="campo"><span class="campo__rotulo">${rotulo}</span>${entrada}</label>`;
}

function desenhar(tela, modo) {
    const config = MODOS[modo];
    const cadastro = modo === "cadastro";
    tela.innerHTML = `
        <div class="login">
            ${logotipo("marca--vertical")}
            <form class="cartao login__cartao" novalidate>
                <div>
                    <h1 class="login__titulo">${config.titulo}</h1>
                    <p class="login__subtitulo">${config.subtitulo}</p>
                </div>
                <div class="login__campos">
                    ${cadastro ? campo("Nome", `<input name="nome" type="text" maxlength="100" autocomplete="name"
                                               placeholder="Seu nome" required>`) : ""}
                    ${campo("E-mail", `<input name="email" type="email" maxlength="254"
                        autocomplete="${cadastro ? "email" : "username"}" placeholder="seu@email.com" required>`)}
                    ${campo(cadastro ? "Senha <small>mínimo de 8 caracteres, com letras e números</small>" : "Senha",
                        `<input name="senha" type="password" maxlength="128" placeholder="••••••••"
                                autocomplete="${cadastro ? "new-password" : "current-password"}" required>`)}
                    ${cadastro ? campo("Confirmar senha", `<input name="confirmacao" type="password" maxlength="128"
                                       placeholder="••••••••" autocomplete="new-password" required>`) : ""}
                </div>
                <button type="submit" class="botao botao--bloco">${config.botao}</button>
                <div class="login__links">${config.links}</div>
            </form>
        </div>`;

    tela.querySelector("[data-modo]").addEventListener("click", (evento) => desenhar(tela, evento.currentTarget.dataset.modo));
    const formulario = tela.querySelector("form");
    formulario.addEventListener("submit", (evento) => {
        evento.preventDefault();
        enviar(formulario, modo);
    });
    formulario.querySelector("input").focus();
}

/** Validação feita também no navegador, para avisar antes de enviar; o servidor valida de novo. */
function validarCadastro({ nome, email, senha }, confirmacao) {
    if (!nome.trim()) {
        return "O nome é obrigatório.";
    }
    if (!FORMATO_EMAIL.test(email.trim())) {
        return "Informe um e-mail válido, como nome@exemplo.com.";
    }
    if (senha.length < 8) {
        return "A senha deve ter pelo menos 8 caracteres.";
    }
    if (!/\p{L}/u.test(senha) || !/\p{N}/u.test(senha)) {
        return "A senha deve ter letras e números.";
    }
    if (senha !== confirmacao) {
        return "As senhas não conferem.";
    }
    return null;
}

/**
 * Depois de entrar, recarrega a página: o site começa do zero com os dados da conta, sem restos
 * de uma conta usada antes no mesmo navegador.
 */
async function enviar(formulario, modo) {
    const campos = formulario.elements;
    let acao;
    if (modo === "login") {
        if (!campos.email.value.trim() || !campos.senha.value) {
            mostrarAviso("Informe o e-mail e a senha.", "erro");
            return;
        }
        acao = () => api.entrar({ email: campos.email.value, senha: campos.senha.value });
    } else {
        const dados = { nome: campos.nome.value, email: campos.email.value, senha: campos.senha.value };
        const erro = validarCadastro(dados, campos.confirmacao.value);
        if (erro) {
            mostrarAviso(erro, "erro");
            return;
        }
        acao = () => api.cadastrarConta(dados);
    }

    const botao = formulario.querySelector('button[type="submit"]');
    botao.disabled = true;
    try {
        await acao();
        location.hash = "#/inicio";
        location.reload();
    } catch (erro) {
        mostrarErro(erro);
        botao.disabled = false;
    }
}

/** Mostra a tela de login, com um aviso opcional (ex.: sessão expirada). */
export function mostrarDeslogado(mensagem) {
    document.body.dataset.estado = "deslogado";
    desenhar(document.getElementById("tela-login"), "login");
    if (mensagem) {
        mostrarAviso(mensagem, "erro");
    }
}

export async function sair() {
    try {
        await api.sair();
    } catch (erro) {
        mostrarErro(erro);
        return;
    }
    location.hash = "";
    location.reload();
}
