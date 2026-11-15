package br.com.ressoa.shared.exception;

/** Usuário autenticado tentando acessar recurso de outro usuário (HTTP 403). */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
