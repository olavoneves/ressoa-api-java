package br.com.ressoa.avaliacao;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.ressoa.avaliacao.dto.AvaliacaoDtos.AvaliacaoRequest;
import br.com.ressoa.avaliacao.dto.AvaliacaoDtos.AvaliacaoResponse;
import br.com.ressoa.bairro.NivelRisco;
import br.com.ressoa.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/avaliacoes")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Avaliações de risco", description = "Nível de risco por bairro e semana (serviço de IA e GESTOR)")
public class AvaliacaoController {

    private final AvaliacaoService service;

    public AvaliacaoController(AvaliacaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Histórico paginado de avaliações (filtros bairroId e nivel)")
    public PageResponse<AvaliacaoResponse> listar(
            @RequestParam(required = false) Long bairroId,
            @RequestParam(required = false) NivelRisco nivel,
            @ParameterObject @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(bairroId, nivel, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma avaliação")
    public AvaliacaoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Registra avaliação (GESTOR / serviço de IA). Se o nível mudar, publica risco.atualizado")
    public AvaliacaoResponse registrar(@Valid @RequestBody AvaliacaoRequest req) {
        return service.registrar(req);
    }
}
