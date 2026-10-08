const formatoMoeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

export function formatarMoeda(valor) {
    return formatoMoeda.format(valor);
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

/**
 * Imagem do produto vinda da loja. Sem imagem, ou se ela não carregar, mostra um ícone no lugar
 * (ver o tratamento de erro de imagens em app.js).
 */
export function imagemProduto(url, descricao, classeExtra = "") {
    const endereco = urlSegura(url);
    if (!endereco) {
        return `<div class="imagem-produto imagem-produto--vazia ${classeExtra}" aria-hidden="true">🎁</div>`;
    }
    return `<img class="imagem-produto ${classeExtra}" src="${escaparHtml(endereco)}" alt="${escaparHtml(descricao)}"
                 loading="lazy" referrerpolicy="no-referrer">`;
}

/** Torna o conteúdo (imagem ou nome do produto) clicável, abrindo a loja em outra aba. */
export function comLink(url, conteudoHtml) {
    const endereco = urlSegura(url);
    return endereco
        ? `<a class="link-produto" href="${escaparHtml(endereco)}" target="_blank" rel="noopener noreferrer">${conteudoHtml}</a>`
        : conteudoHtml;
}

export function linkCompra(url, texto = "Comprar na loja ↗") {
    const endereco = urlSegura(url);
    return endereco
        ? `<a class="botao botao--comprar botao--pequeno" href="${escaparHtml(endereco)}" target="_blank"
              rel="noopener noreferrer">${texto}</a>`
        : "";
}

export function textoParaLista(texto) {
    return texto.split(",").map((item) => item.trim()).filter(Boolean);
}

export function listaParaTexto(lista) {
    return lista.join(", ");
}

/** Campo vazio vira null, para o servidor informar que é obrigatório. */
export function numeroOuNulo(valor) {
    return valor === "" ? null : Number(valor);
}

export function textoOuNulo(valor) {
    return valor === "" ? null : valor;
}

export function etiquetas(itens, classeExtra = "") {
    if (itens.length === 0) {
        return "-";
    }
    const html = itens
        .map((item) => `<span class="etiqueta ${classeExtra}">${escaparHtml(item)}</span>`)
        .join("");
    return `<span class="etiquetas">${html}</span>`;
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
