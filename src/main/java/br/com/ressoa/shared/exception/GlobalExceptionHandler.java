package br.com.ressoa.shared.exception;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

/**
 * Tratamento global de erros. Todas as respostas seguem a RFC 9457 (Problem Details),
 * o mesmo formato usado pela API .NET, para o app tratar erros de forma uniforme.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TIPO_BASE = "https://ressoa.app/erros/";

    public record ErroCampo(String campo, String mensagem) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErroCampo> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErroCampo(e.getField(), e.getDefaultMessage()))
                .toList();
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "validacao", "Dados inválidos",
                "Um ou mais campos não passaram na validação.", req.getRequestURI());
        pd.setProperty("erros", erros);
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail validacaoParametro(ConstraintViolationException ex, HttpServletRequest req) {
        List<ErroCampo> erros = ex.getConstraintViolations().stream()
                .map(v -> new ErroCampo(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "validacao", "Dados inválidos",
                "Um ou mais parâmetros não passaram na validação.", req.getRequestURI());
        pd.setProperty("erros", erros);
        return pd;
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    ProblemDetail corpoInvalido(Exception ex, HttpServletRequest req) {
        return problema(HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida",
                "O corpo ou os parâmetros da requisição estão em formato inválido.", req.getRequestURI());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Recurso não encontrado", ex.getMessage(),
                req.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail rotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Recurso não encontrado",
                "A rota solicitada não existe.", req.getRequestURI());
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail conflito(ConflitoException ex, HttpServletRequest req) {
        return problema(HttpStatus.CONFLICT, "conflito", "Conflito", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violação de integridade: {}", ex.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "conflito", "Conflito",
                "A operação viola uma restrição de integridade (registro duplicado ou em uso).",
                req.getRequestURI());
    }

    @ExceptionHandler(RegraNegocioException.class)
    ProblemDetail regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "regra-negocio", "Regra de negócio violada",
                ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail credenciais(BadCredentialsException ex, HttpServletRequest req) {
        return problema(HttpStatus.UNAUTHORIZED, "nao-autenticado", "Não autenticado",
                "E-mail ou senha inválidos.", req.getRequestURI());
    }

    @ExceptionHandler({ AcessoNegadoException.class, AuthorizationDeniedException.class })
    ProblemDetail acessoNegado(RuntimeException ex, HttpServletRequest req) {
        String detalhe = ex instanceof AcessoNegadoException ? ex.getMessage()
                : "Seu perfil não tem permissão para esta operação.";
        return problema(HttpStatus.FORBIDDEN, "acesso-negado", "Acesso negado", detalhe, req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "erro-interno", "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.", req.getRequestURI());
    }

    /** Fábrica usada também pelos handlers de 401/403 do Spring Security. */
    public static ProblemDetail problema(HttpStatus status, String tipo, String titulo, String detalhe,
            String caminho) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalhe);
        pd.setType(URI.create(TIPO_BASE + tipo));
        pd.setTitle(titulo);
        pd.setInstance(URI.create(caminho));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
