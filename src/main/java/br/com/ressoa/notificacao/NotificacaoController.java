package br.com.ressoa.notificacao;

import java.util.Map;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.ressoa.notificacao.dto.NotificacaoResponse;
import br.com.ressoa.shared.security.UsuarioAutenticado;
import br.com.ressoa.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/notificacoes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificações", description = "Alertas ao morador quando o risco do bairro sobe")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os alertas do usuário (filtro opcional lida=true|false)")
    public PageResponse<NotificacaoResponse> listar(Authentication auth,
            @RequestParam(required = false) Boolean lida,
            @ParameterObject @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(UsuarioAutenticado.de(auth).id(), lida, pageable);
    }

    @GetMapping("/nao-lidas/contagem")
    @Operation(summary = "Quantidade de alertas não lidos (badge do app)")
    public Map<String, Long> contarNaoLidas(Authentication auth) {
        return Map.of("naoLidas", service.contarNaoLidas(UsuarioAutenticado.de(auth).id()));
    }

    @PatchMapping("/{id}/lida")
    @Operation(summary = "Marca um alerta como lido")
    public NotificacaoResponse marcarComoLida(Authentication auth, @PathVariable Long id) {
        return service.marcarComoLida(UsuarioAutenticado.de(auth).id(), id);
    }
}
