package exception;

public abstract class RegistroNaoEncontradoException extends RuntimeException {

    protected RegistroNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
