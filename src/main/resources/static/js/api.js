// Comunicação com a API do servidor. Erros da API viram exceções com a mensagem enviada pelo servidor.

async function requisitar(metodo, caminho, corpo) {
    const opcoes = { method: metodo, headers: {} };
    if (corpo !== undefined) {
        opcoes.headers["Content-Type"] = "application/json";
        opcoes.body = JSON.stringify(corpo);
    }

    let resposta;
    try {
        resposta = await fetch(caminho, opcoes);
    } catch {
        throw new Error("Não foi possível conectar ao servidor.");
    }

    if (resposta.status === 204) {
        return null;
    }
    const dados = await resposta.json().catch(() => null);
    if (!resposta.ok) {
        throw new Error(dados?.mensagem ?? `Erro inesperado (código ${resposta.status}).`);
    }
    return dados;
}

export const api = {
    opcoes: () => requisitar("GET", "/api/opcoes"),

    listarPessoas: () => requisitar("GET", "/api/pessoas"),
    cadastrarPessoa: (dados) => requisitar("POST", "/api/pessoas", dados),
    alterarPessoa: (id, dados) => requisitar("PUT", `/api/pessoas/${id}`, dados),
    removerPessoa: (id) => requisitar("DELETE", `/api/pessoas/${id}`),

    listarPresentes: () => requisitar("GET", "/api/presentes"),
    cadastrarPresente: (dados) => requisitar("POST", "/api/presentes", dados),
    alterarPresente: (id, dados) => requisitar("PUT", `/api/presentes/${id}`, dados),
    removerPresente: (id) => requisitar("DELETE", `/api/presentes/${id}`),

    sugestoesDoCatalogo: (pessoaId) => requisitar("GET", `/api/pessoas/${pessoaId}/sugestoes`),
    ideiasComIa: (pessoaId) => requisitar("POST", `/api/pessoas/${pessoaId}/ideias-ia`),

    listarFavoritos: (pessoaId) => requisitar("GET", `/api/favoritos${filtroPessoa(pessoaId)}`),
    favoritar: (dados) => requisitar("POST", "/api/favoritos", dados),
    removerFavorito: (id) => requisitar("DELETE", `/api/favoritos/${id}`),

    listarHistorico: (pessoaId) => requisitar("GET", `/api/historico${filtroPessoa(pessoaId)}`),
};

function filtroPessoa(pessoaId) {
    return pessoaId ? `?pessoaId=${pessoaId}` : "";
}
