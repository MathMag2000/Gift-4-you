// Ícones de traço fino usados no site, no mesmo estilo do wireframe.

function svg(tamanho, conteudo, preenchido = false) {
    return `<svg width="${tamanho}" height="${tamanho}" viewBox="0 0 24 24" fill="${preenchido ? "currentColor" : "none"}"
                 stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
                 aria-hidden="true" focusable="false">${conteudo}</svg>`;
}

const CORACAO = '<path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 000-7.78z"/>';

export const icones = {
    inicio: (t = 22) => svg(t, '<path d="M3 9.75L12 3l9 6.75V21a.75.75 0 01-.75.75H15v-6H9v6H3.75A.75.75 0 013 21V9.75z"/>'),
    coracao: (t = 22, preenchido = false) => svg(t, CORACAO, preenchido),
    relogio: (t = 22) => svg(t, '<circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 15"/>'),
    configuracoes: (t = 22) => svg(t, '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>'),
    pessoa: (t = 22) => svg(t, '<path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>'),
    presente: (t = 22) => svg(t, '<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13"/><path d="M19 12v7a2 2 0 01-2 2H7a2 2 0 01-2-2v-7"/><path d="M7.5 8a2.5 2.5 0 010-5C11 3 12 8 12 8s1-5 4.5-5a2.5 2.5 0 010 5"/>'),
    direita: (t = 18) => svg(t, '<polyline points="9 18 15 12 9 6"/>'),
    esquerda: (t = 18) => svg(t, '<polyline points="15 18 9 12 15 6"/>'),
    externo: (t = 13) => svg(t, '<path d="M18 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/>'),
    sair: (t = 18) => svg(t, '<path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>'),
};

/** Quadro com um "X", o espaço reservado para imagens no wireframe. */
export function marcadorImagem(classeExtra = "") {
    return `<div class="marcador-imagem ${classeExtra}" aria-hidden="true">
                <svg viewBox="0 0 100 100" preserveAspectRatio="none">
                    <line x1="0" y1="0" x2="100" y2="100" vector-effect="non-scaling-stroke"/>
                    <line x1="100" y1="0" x2="0" y2="100" vector-effect="non-scaling-stroke"/>
                </svg>
            </div>`;
}

/** Logotipo: o quadro com "X" do wireframe, com nome e slogan. */
export function logotipo(classeExtra = "") {
    return `<div class="marca ${classeExtra}">
                <div class="marca__logo" aria-hidden="true">
                    <svg viewBox="0 0 40 40"><line x1="0" y1="0" x2="40" y2="40"/><line x1="40" y1="0" x2="0" y2="40"/></svg>
                </div>
                <div class="marca__texto">
                    <span class="marca__nome">PRESENTE IDEAL</span>
                    <span class="marca__slogan">Sugestões que conectam</span>
                </div>
            </div>`;
}
