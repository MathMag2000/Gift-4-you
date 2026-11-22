import { api } from "../api.js";
import { sair } from "../conta.js";
import { bloco, cabecalhoPagina, campo } from "../componentes/formulario.js";
import { icones } from "../componentes/icones.js";
import { comBotaoOcupado, escaparHtml, mostrarAviso, mostrarErro } from "../util.js";

/** Mesmas regras do cadastro, conferidas também no servidor. */
function validarNovaSenha(senhaAtual, novaSenha, confirmacao) {
    if (!senhaAtual) {
        return "Informe a senha atual.";
    }
    if (novaSenha.length < 8) {
        return "A nova senha deve ter pelo menos 8 caracteres.";
    }
    if (!/\p{L}/u.test(novaSenha) || !/\p{N}/u.test(novaSenha)) {
        return "A nova senha deve ter letras e números.";
    }
    if (novaSenha !== confirmacao) {
        return "As senhas não conferem.";
    }
    if (novaSenha === senhaAtual) {
        return "A nova senha deve ser diferente da atual.";
    }
    return null;
}

/** O nome também aparece no rodapé do menu lateral. */
function atualizarNomeNoMenu(nome) {
    const usuario = document.querySelector(".lateral__usuario");
    if (usuario) {
        usuario.textContent = nome;
    }
}

export async function telaConfiguracoes(principal) {
    const conta = await api.contaAtual();
    principal.innerHTML = `
        ${cabecalhoPagina("Configurações", "Administre os dados da sua conta.")}
        <div class="formulario-largo">
            <form id="form-nome" novalidate>
                ${bloco("Dados da conta", `
                    <div class="linha-campos">
                        ${campo("Nome", `<input name="nome" type="text" maxlength="100" autocomplete="name"
                            value="${escaparHtml(conta?.nome)}" required>`, "campo--flexivel")}
                        ${campo("E-mail", `<input type="email" value="${escaparHtml(conta?.email)}" disabled>`,
                            "campo--flexivel")}
                    </div>
                    <p class="bloco__ajuda">O e-mail é usado para entrar e não pode ser alterado.</p>
                    <div class="acoes">
                        <button type="submit" class="botao">Salvar nome</button>
                    </div>`)}
            </form>

            <form id="form-senha" novalidate>
                ${bloco("Alterar senha", `
                    <input type="email" name="usuario" value="${escaparHtml(conta?.email)}" autocomplete="username" hidden>
                    ${campo("Senha atual", `<input name="senhaAtual" type="password" maxlength="128"
                        autocomplete="current-password" placeholder="••••••••" required>`)}
                    <div class="linha-campos">
                        ${campo("Nova senha <small>mínimo de 8 caracteres, com letras e números</small>",
                            `<input name="novaSenha" type="password" maxlength="128" autocomplete="new-password"
                                    placeholder="••••••••" required>`, "campo--flexivel")}
                        ${campo("Confirmar nova senha", `<input name="confirmacao" type="password" maxlength="128"
                            autocomplete="new-password" placeholder="••••••••" required>`, "campo--flexivel")}
                    </div>
                    <div class="acoes">
                        <button type="submit" class="botao">Alterar senha</button>
                    </div>`)}
            </form>

            <div class="dica">
                <p>As pessoas, o catálogo de presentes, os favoritos e o histórico são visíveis somente para a sua conta.</p>
            </div>
            <div class="acoes">
                <button type="button" class="botao botao--contorno-forte" data-acao="sair">${icones.sair()} Sair da conta</button>
            </div>
        </div>`;

    principal.querySelector('[data-acao="sair"]').addEventListener("click", sair);

    const formNome = principal.querySelector("#form-nome");
    formNome.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const nome = formNome.elements.nome.value;
        if (!nome.trim()) {
            mostrarAviso("O nome é obrigatório.", "erro");
            return;
        }
        try {
            const atualizada = await comBotaoOcupado(formNome.querySelector('button[type="submit"]'), "Salvando...",
                () => api.alterarNomeConta(nome));
            formNome.elements.nome.value = atualizada.nome;
            atualizarNomeNoMenu(atualizada.nome);
            mostrarAviso("Nome atualizado.");
        } catch (erro) {
            mostrarErro(erro);
        }
    });

    const formSenha = principal.querySelector("#form-senha");
    formSenha.addEventListener("submit", async (evento) => {
        evento.preventDefault();
        const campos = formSenha.elements;
        const erro = validarNovaSenha(campos.senhaAtual.value, campos.novaSenha.value, campos.confirmacao.value);
        if (erro) {
            mostrarAviso(erro, "erro");
            return;
        }
        try {
            await comBotaoOcupado(formSenha.querySelector('button[type="submit"]'), "Alterando...",
                () => api.alterarSenha({ senhaAtual: campos.senhaAtual.value, novaSenha: campos.novaSenha.value }));
            formSenha.reset();
            mostrarAviso("Senha alterada.");
        } catch (falha) {
            mostrarErro(falha);
        }
    });
}
