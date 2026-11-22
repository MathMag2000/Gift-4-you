// Comunicação com a API do servidor. Erros da API viram exceções com a mensagem enviada pelo servidor.

/** Disparado quando a API responde 401 fora da conta, ou seja, a sessão expirou ou foi encerrada. */
export const EVENTO_SESSAO_EXPIRADA = "gift4you:sessao-expirada";

export class ErroApi extends Error {

    constructor(mensagem, status) {
        super(mensagem);
        this.status = status;
    }
}

/**
 * @param avisarSessaoExpirada em 401, avisa o site para mostrar a tela de login. Desligado nas rotas da
 *        conta, onde 401 significa apenas senha incorreta ou ninguém logado.
 */
async function requisitar(metodo, caminho, corpo, { avisarSessaoExpirada = true } = {}) {
    const opcoes = { method: metodo, headers: {} };
    if (corpo !== undefined) {
        opcoes.headers["Content-Type"] = "application/json";
        opcoes.body = JSON.stringify(corpo);
    }

    let resposta;
    try {
        resposta = await fetch(caminho, opcoes);
    } catch {
        throw new ErroApi("Não foi possível conectar ao servidor.", 0);
    }

    if (resposta.status === 204) {
        return null;
    }
    const dados = await resposta.json().catch(() => null);
    if (!resposta.ok) {
        if (resposta.status === 401 && avisarSessaoExpirada) {
            window.dispatchEvent(new CustomEvent(EVENTO_SESSAO_EXPIRADA));
        }
        throw new ErroApi(dados?.mensagem ?? `Erro inesperado (código ${resposta.status}).`, resposta.status);
    }
    return dados;
}

const SEM_AVISO = { avisarSessaoExpirada: false };

export const api = {
    opcoes: () => requisitar("GET", "/api/opcoes"),

    /** Conta logada, ou null se ninguém estiver logado. */
    contaAtual: () => requisitar("GET", "/api/conta", undefined, SEM_AVISO).catch((erro) => {
        if (erro.status === 401) {
            return null;
        }
        throw erro;
    }),
    cadastrarConta: (dados) => requisitar("POST", "/api/conta/cadastro", dados, SEM_AVISO),
    entrar: (dados) => requisitar("POST", "/api/conta/login", dados, SEM_AVISO),
    sair: () => requisitar("POST", "/api/conta/logout", undefined, SEM_AVISO),
    alterarNomeConta: (nome) => requisitar("PUT", "/api/conta/nome", { nome }),
    alterarSenha: (dados) => requisitar("PUT", "/api/conta/senha", dados),

    listarPessoas: () => requisitar("GET", "/api/pessoas"),
    buscarPessoa: (id) => requisitar("GET", `/api/pessoas/${id}`),
    cadastrarPessoa: (dados) => requisitar("POST", "/api/pessoas", dados),
    alterarPessoa: (id, dados) => requisitar("PUT", `/api/pessoas/${id}`, dados),
    removerPessoa: (id) => requisitar("DELETE", `/api/pessoas/${id}`),

    listarPresentes: () => requisitar("GET", "/api/presentes"),
    buscarPresente: (id) => requisitar("GET", `/api/presentes/${id}`),
    cadastrarPresente: (dados) => requisitar("POST", "/api/presentes", dados),
    alterarPresente: (id, dados) => requisitar("PUT", `/api/presentes/${id}`, dados),
    removerPresente: (id) => requisitar("DELETE", `/api/presentes/${id}`),

    sugestoesDoCatalogo: (pessoaId) => requisitar("GET", `/api/pessoas/${pessoaId}/sugestoes`),
    presentesDaPessoa: (pessoaId) => requisitar("GET", `/api/pessoas/${pessoaId}/presentes`),
    ideiasComIa: (pessoaId) => requisitar("POST", `/api/pessoas/${pessoaId}/ideias-ia`),

    listarFavoritos: (pessoaId) => requisitar("GET", `/api/favoritos${filtroPessoa(pessoaId)}`),
    favoritar: (dados) => requisitar("POST", "/api/favoritos", dados),
    removerFavorito: (id) => requisitar("DELETE", `/api/favoritos/${id}`),

    listarHistorico: (pessoaId) => requisitar("GET", `/api/historico${filtroPessoa(pessoaId)}`),
};

function filtroPessoa(pessoaId) {
    return pessoaId ? `?pessoaId=${pessoaId}` : "";
}
