package exception;

public class PresenteNaoEncontradoException extends RegistroNaoEncontradoException {

    public PresenteNaoEncontradoException(int id) {
        super("Presente com id " + id + " não encontrado.");
    }
}
