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
