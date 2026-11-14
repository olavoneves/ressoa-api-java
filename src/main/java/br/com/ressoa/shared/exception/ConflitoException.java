package br.com.ressoa.shared.exception;

/** Violação de unicidade ou de integridade de negócio (HTTP 409). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
