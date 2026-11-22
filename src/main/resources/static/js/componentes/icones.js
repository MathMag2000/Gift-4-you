// Ícones de traço do site, todos no mesmo estilo (linhas arredondadas, grade de 24 px, como os do Lucide).

function svg(tamanho, conteudo, preenchido = false) {
    return `<svg class="icone" width="${tamanho}" height="${tamanho}" viewBox="0 0 24 24" fill="${preenchido ? "currentColor" : "none"}"
                 stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"
                 aria-hidden="true" focusable="false">${conteudo}</svg>`;
}

const CORACAO = '<path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 000-7.78z"/>';
const PRESENTE = '<rect x="3" y="8" width="18" height="4" rx="1"/><path d="M12 8v13"/><path d="M19 12v7a2 2 0 01-2 2H7a2 2 0 01-2-2v-7"/><path d="M7.5 8a2.5 2.5 0 010-5C11 3 12 8 12 8s1-5 4.5-5a2.5 2.5 0 010 5"/>';

export const icones = {
    inicio: (t = 20) => svg(t, '<path d="M3 9.75L12 3l9 6.75V21a.75.75 0 01-.75.75H15v-6H9v6H3.75A.75.75 0 013 21V9.75z"/>'),
    coracao: (t = 20, preenchido = false) => svg(t, CORACAO, preenchido),
    relogio: (t = 20) => svg(t, '<circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 15"/>'),
    configuracoes: (t = 20) => svg(t, '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>'),
    pessoa: (t = 20) => svg(t, '<path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/>'),
    presente: (t = 20) => svg(t, PRESENTE),
    direita: (t = 18) => svg(t, '<polyline points="9 18 15 12 9 6"/>'),
    esquerda: (t = 18) => svg(t, '<polyline points="15 18 9 12 15 6"/>'),
    seta: (t = 16) => svg(t, '<line x1="5" y1="12" x2="19" y2="12"/><polyline points="13 6 19 12 13 18"/>'),
    voltar: (t = 16) => svg(t, '<line x1="19" y1="12" x2="5" y2="12"/><polyline points="11 18 5 12 11 6"/>'),
    mais: (t = 16) => svg(t, '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>'),
    fechar: (t = 18) => svg(t, '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>'),
    brilho: (t = 14) => svg(t, '<path d="M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z"/><path d="M19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z"/>'),
    copiar: (t = 15) => svg(t, '<rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/>'),
    confirmado: (t = 15) => svg(t, '<polyline points="20 6 9 17 4 12"/>'),
    externo: (t = 14) => svg(t, '<path d="M18 13v6a2 2 0 01-2 2H5a2 2 0 01-2-2V8a2 2 0 012-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/>'),
    sair: (t = 18) => svg(t, '<path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>'),
};

/** Espaço reservado para imagens: fundo suave com um presente desenhado no centro. */
export function marcadorImagem(classeExtra = "") {
    return `<div class="marcador-imagem ${classeExtra}" aria-hidden="true">${icones.presente(28)}</div>`;
}

/** Logotipo: selo verde com um presente, nome e slogan. */
export function logotipo(classeExtra = "") {
    return `<div class="marca ${classeExtra}">
                <div class="marca__logo" aria-hidden="true">${icones.presente(24)}</div>
                <div class="marca__texto">
                    <span class="marca__nome">PRESENTE IDEAL</span>
                    <span class="marca__slogan">Sugestões que conectam</span>
                </div>
            </div>`;
}
