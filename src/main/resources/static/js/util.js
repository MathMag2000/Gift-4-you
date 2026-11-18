import { marcadorImagem } from "./componentes/icones.js";

const formatoMoeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

export function formatarMoeda(valor) {
    return valor === null || valor === undefined ? "—" : formatoMoeda.format(valor);
}

const formatoDataHora = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });

/** Recebe a data no formato do servidor (ex.: 2026-10-08T17:15:03). */
export function formatarDataHora(texto) {
    return formatoDataHora.format(new Date(texto));
}

/** Evita que textos digitados pelo usuário sejam interpretados como HTML. */
export function escaparHtml(texto) {
    return String(texto ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

/** Só aceita endereços http(s), evitando links como "javascript:" mesmo que cheguem do servidor. */
export function urlSegura(url) {
    return typeof url === "string" && /^https?:\/\//i.test(url) ? url : null;
}

/** Endereço da loja sem "https://", para exibir (ex.: www.amazon.com.br/dp/B0...). */
export function enderecoParaExibir(url) {
    const endereco = urlSegura(url);
    if (!endereco) {
        return "";
    }
    let legivel = endereco;
    try {
        legivel = decodeURI(endereco); // "%20" vira espaço, "%C3%A9" vira "é"
    } catch {
        // Endereço com codificação inválida: mostra como veio.
    }
    return legivel.replace(/^https?:\/\//i, "").replace(/\/$/, "");
}

/**
 * Imagem do produto vinda da loja. Sem imagem, ou se ela não carregar, mostra o quadro com "X"
 * do wireframe (ver o tratamento de erro de imagens em app.js).
 */
export function imagemProduto(url, descricao, classeExtra = "") {
    const endereco = urlSegura(url);
    if (!endereco) {
        return marcadorImagem(classeExtra);
    }
    return `<img class="imagem-produto ${classeExtra}" src="${escaparHtml(endereco)}" alt="${escaparHtml(descricao)}"
                 loading="lazy" referrerpolicy="no-referrer">`;
}

/** Quadro com "X" que substitui uma imagem que não carregou, mantendo as classes de tamanho dela. */
export function marcadorErroImagem(classes) {
    const modelo = document.createElement("template");
    modelo.innerHTML = marcadorImagem(classes.replace("imagem-produto", "").trim());
    return modelo.content.firstElementChild;
}

export function textoParaLista(texto) {
    return texto.split(",").map((item) => item.trim()).filter(Boolean);
}

export function listaParaTexto(lista) {
    return lista.join(", ");
}

/** Campo vazio vira null, para o servidor informar que é obrigatório. */
export function numeroOuNulo(valor) {
    const texto = String(valor).trim().replace(/^R\$\s*/i, "");
    if (texto === "") {
        return null;
    }
    // Aceita "1.299,90" e "1299.90".
    const normalizado = texto.includes(",") ? texto.replaceAll(".", "").replace(",", ".") : texto;
    return Number(normalizado);
}

export function textoOuNulo(valor) {
    return valor === "" ? null : valor;
}

export function capitalizar(texto) {
    return texto ? texto.charAt(0).toUpperCase() + texto.slice(1) : texto;
}

/** Lista para leitura: "café, música e livros". */
export function listaPorExtenso(itens) {
    if (!itens || itens.length === 0) {
        return "";
    }
    return itens.length === 1 ? itens[0] : `${itens.slice(0, -1).join(", ")} e ${itens.at(-1)}`;
}

let temporizadorAviso;

export function mostrarAviso(mensagem, tipo = "sucesso") {
    const aviso = document.getElementById("aviso");
    aviso.textContent = mensagem;
    aviso.classList.toggle("aviso--erro", tipo === "erro");
    aviso.hidden = false;
    clearTimeout(temporizadorAviso);
    temporizadorAviso = setTimeout(() => (aviso.hidden = true), tipo === "erro" ? 6000 : 3000);
}

export function mostrarErro(erro) {
    mostrarAviso(erro.message, "erro");
}

/** Desabilita o botão enquanto a ação roda, mostrando um texto de espera. */
export async function comBotaoOcupado(botao, textoEspera, acao) {
    const textoOriginal = botao.innerHTML;
    botao.disabled = true;
    botao.textContent = textoEspera;
    try {
        return await acao();
    } finally {
        botao.disabled = false;
        botao.innerHTML = textoOriginal;
    }
}
