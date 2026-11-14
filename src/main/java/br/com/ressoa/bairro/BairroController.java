package br.com.ressoa.bairro;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.ressoa.bairro.dto.BairroDtos.BairroRequest;
import br.com.ressoa.bairro.dto.BairroDtos.BairroResponse;
import br.com.ressoa.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bairros")
@Tag(name = "Bairros", description = "Territórios monitorados. Leitura pública (cadastro do morador); escrita só GESTOR")
public class BairroController {

    private final BairroService service;

    public BairroController(BairroService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista bairros paginados, com filtros opcionais")
    public PageResponse<BairroResponse> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) NivelRisco nivel,
            @ParameterObject @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(nome, municipio, uf, nivel, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um bairro com o nível de risco atual")
    public BairroResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cadastra bairro (GESTOR)")
    public BairroResponse criar(@Valid @RequestBody BairroRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Atualiza bairro (GESTOR)")
    public BairroResponse atualizar(@PathVariable Long id, @Valid @RequestBody BairroRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('GESTOR')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Exclui bairro sem vínculos (GESTOR)")
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }
}
