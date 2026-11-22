// Janela sobreposta (detalhes do produto e confirmações). Fecha com Esc, com o botão de fechar ou clicando fora.

/**
 * @returns {{ caixa: HTMLElement, fechar: () => void }}
 */
export function abrirModal(html, { classe = "", rotulo = "", aoFechar } = {}) {
    const fundo = document.createElement("div");
    fundo.className = "modal-fundo";
    fundo.innerHTML = `<div class="modal ${classe}" role="dialog" aria-modal="true" aria-label="${rotulo.replaceAll('"', "&quot;")}">${html}</div>`;
    const caixa = fundo.querySelector(".modal");
    const focoAnterior = document.activeElement;

    function fechar() {
        document.removeEventListener("keydown", aoTeclar);
        fundo.remove();
        focoAnterior?.focus?.();
        aoFechar?.();
    }

    function aoTeclar(evento) {
        if (evento.key === "Escape") {
            fechar();
        }
    }

    fundo.addEventListener("click", (evento) => {
        if (evento.target === fundo || evento.target.closest('[data-acao="fechar"]')) {
            fechar();
        }
    });
    document.addEventListener("keydown", aoTeclar);
    document.getElementById("modal-raiz").append(fundo);
    (caixa.querySelector("button, a, input") ?? caixa).focus();
    return { caixa, fechar };
}

/** Pergunta de confirmação, como "Deseja realmente remover esta pessoa?". Resolve com true se confirmada. */
export function confirmar({ titulo, texto, botao = "Remover" }) {
    return new Promise((resolver) => {
        let confirmado = false;
        const { caixa, fechar } = abrirModal(`
            <div class="confirmacao">
                <p class="confirmacao__titulo">${titulo}</p>
                <p class="confirmacao__texto">${texto}</p>
                <div class="confirmacao__acoes">
                    <button type="button" class="botao" data-acao="confirmar">${botao}</button>
                    <button type="button" class="botao botao--contorno" data-acao="fechar">Cancelar</button>
                </div>
            </div>`, { classe: "modal--confirmacao", rotulo: titulo, aoFechar: () => resolver(confirmado) });
        caixa.querySelector('[data-acao="confirmar"]').addEventListener("click", () => {
            confirmado = true;
            fechar();
        });
    });
}
