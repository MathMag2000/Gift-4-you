/**
 * Controla o modo do formulário (cadastro ou alteração), compartilhado por pessoas e presentes.
 */
export class ControleFormulario {

    constructor(formulario, titulo, nomeEntidade) {
        this.formulario = formulario;
        this.titulo = titulo;
        this.nomeEntidade = nomeEntidade;
        this.botaoSalvar = formulario.querySelector('button[type="submit"]');
        this.botaoCancelar = formulario.querySelector('[data-acao="cancelar"]');
        this.idEmEdicao = null;
        this.botaoCancelar.addEventListener("click", () => this.voltarParaCadastro());
    }

    editar(id) {
        this.idEmEdicao = id;
        this.titulo.textContent = `Alterar ${this.nomeEntidade}`;
        this.botaoSalvar.textContent = "Salvar alterações";
        this.botaoCancelar.hidden = false;
        this.formulario.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    voltarParaCadastro() {
        this.idEmEdicao = null;
        this.formulario.reset();
        this.titulo.textContent = `Cadastrar ${this.nomeEntidade}`;
        this.botaoSalvar.textContent = "Cadastrar";
        this.botaoCancelar.hidden = true;
    }

    emEdicao() {
        return this.idEmEdicao !== null;
    }

    async executarSalvando(acao) {
        this.botaoSalvar.disabled = true;
        try {
            await acao();
        } finally {
            this.botaoSalvar.disabled = false;
        }
    }
}
