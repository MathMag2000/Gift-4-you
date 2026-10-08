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

    /** Desabilita o botão enquanto salva, mostrando um texto de espera. */
    async executarSalvando(acao, textoEspera = "Salvando...") {
        const textoOriginal = this.botaoSalvar.textContent;
        this.botaoSalvar.disabled = true;
        this.botaoSalvar.textContent = textoEspera;
        try {
            await acao();
        } finally {
            this.botaoSalvar.disabled = false;
            if (this.botaoSalvar.textContent === textoEspera) {
                this.botaoSalvar.textContent = textoOriginal;
            }
        }
    }
}
