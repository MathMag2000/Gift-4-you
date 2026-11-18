import { api } from "../api.js";
import { EstadoFavoritos } from "../favoritar.js";
import { cabecalhoPagina, nomeDaPessoa, seletorPessoas, vazio } from "../componentes/formulario.js";
import { icones } from "../componentes/icones.js";
import { renderizarProdutos } from "../componentes/produtos.js";
import { mostrarErro } from "../util.js";

export async function telaFavoritos(principal, { consulta }) {
    const pessoas = await api.listarPessoas();
    const pessoaInicial = consulta.get("pessoa") ?? "";

    principal.innerHTML = `
        ${cabecalhoPagina("Favoritos", "Produtos que você salvou para ver depois.",
            pessoas.length > 1 ? seletorPessoas("filtro-favoritos", pessoas, pessoaInicial) : "")}
        <div id="lista-favoritos"></div>`;

    const lista = principal.querySelector("#lista-favoritos");
    const filtro = principal.querySelector("#filtro-favoritos");
    const favoritos = new EstadoFavoritos();

    async function carregar() {
        const pessoaId = (filtro ? filtro.value : pessoaInicial) || undefined;
        await favoritos.carregar(pessoaId);
        if (favoritos.favoritos.length === 0) {
            lista.innerHTML = vazio(icones.coracao(32),
                "Nenhum favorito ainda.<br>Clique no coração de qualquer produto para salvá-lo aqui.");
            return;
        }
        // Os cartões refletem a lista do servidor: desfavoritar recarrega e o cartão some.
        const entradas = favoritos.favoritos.map((favorito) => ({
            item: favorito.item, origem: favorito.origem, pessoaId: favorito.pessoaId,
            nota: `Para ${nomeDaPessoa(pessoas, favorito.pessoaId)}`,
        }));
        lista.innerHTML = `<div class="grade-produtos"></div>`;
        renderizarProdutos(lista.firstElementChild, entradas, favoritos, { aoAlternarFavorito: carregar });
    }

    filtro?.addEventListener("change", () => carregar().catch(mostrarErro));
    await carregar();
}
