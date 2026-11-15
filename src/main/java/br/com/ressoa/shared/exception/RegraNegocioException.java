package br.com.ressoa.shared.exception;

/** Requisição válida sintaticamente, mas que fere uma regra de negócio (HTTP 422). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
