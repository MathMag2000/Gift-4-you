// Tela de login e cadastro de conta, e o botão "Sair".
import { api } from "./api.js";
import { mostrarAviso, mostrarErro } from "./util.js";

const formLogin = document.getElementById("form-login");
const formCadastro = document.getElementById("form-cadastro");
const abasConta = document.querySelectorAll("[data-conta]");

const FORMATO_EMAIL = /^[^\s@]+@[^\s@.]+(\.[^\s@.]+)+$/;

export function iniciarConta() {
    abasConta.forEach((aba) => aba.addEventListener("click", () => mostrarFormulario(aba.dataset.conta)));
    formLogin.addEventListener("submit", entrar);
    formCadastro.addEventListener("submit", cadastrar);
    document.getElementById("botao-sair").addEventListener("click", sair);
}

/** Mostra o site da conta logada. */
export function mostrarLogado(conta) {
    document.getElementById("nome-usuario").textContent = `Olá, ${conta.nome}`;
    document.body.dataset.estado = "logado";
}

/** Mostra a tela de login, com um aviso opcional (ex.: sessão expirada). */
export function mostrarDeslogado(mensagem) {
    document.body.dataset.estado = "deslogado";
    mostrarFormulario("login");
    if (mensagem) {
        mostrarAviso(mensagem, "erro");
    }
}

function mostrarFormulario(nome) {
    abasConta.forEach((aba) => {
        const ativa = aba.dataset.conta === nome;
        aba.classList.toggle("aba--ativa", ativa);
        aba.setAttribute("aria-selected", ativa);
    });
    formLogin.hidden = nome !== "login";
    formCadastro.hidden = nome !== "cadastro";
    (nome === "login" ? formLogin : formCadastro).querySelector("input").focus();
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
async function enviar(formulario, acao) {
    const botao = formulario.querySelector('button[type="submit"]');
    botao.disabled = true;
    try {
        await acao();
        window.location.reload();
    } catch (erro) {
        mostrarErro(erro);
        botao.disabled = false;
    }
}

function entrar(evento) {
    evento.preventDefault();
    const campos = formLogin.elements;
    if (!campos.email.value.trim() || !campos.senha.value) {
        mostrarAviso("Informe o e-mail e a senha.", "erro");
        return;
    }
    return enviar(formLogin, () => api.entrar({ email: campos.email.value, senha: campos.senha.value }));
}

function cadastrar(evento) {
    evento.preventDefault();
    const campos = formCadastro.elements;
    const dados = { nome: campos.nome.value, email: campos.email.value, senha: campos.senha.value };
    const erro = validarCadastro(dados, campos.confirmacao.value);
    if (erro) {
        mostrarAviso(erro, "erro");
        return;
    }
    return enviar(formCadastro, () => api.cadastrarConta(dados));
}

async function sair() {
    try {
        await api.sair();
    } catch (erro) {
        mostrarErro(erro);
        return;
    }
    window.location.reload();
}
