package br.com.ressoa.dispositivo;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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

import br.com.ressoa.dispositivo.dto.DispositivoDtos.DispositivoAtualizarRequest;
import br.com.ressoa.dispositivo.dto.DispositivoDtos.DispositivoCriarRequest;
import br.com.ressoa.dispositivo.dto.DispositivoDtos.DispositivoResponse;
import br.com.ressoa.shared.security.UsuarioAutenticado;
import br.com.ressoa.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/dispositivos")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Aparelhos", description = "Aparelhos Ressoa. CIDADAO gerencia os seus; GESTOR gerencia todos")
public class DispositivoController {

    private final DispositivoService service;

    public DispositivoController(DispositivoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista aparelhos paginados (os seus, ou todos para GESTOR)")
    public PageResponse<DispositivoResponse> listar(Authentication auth,
            @RequestParam(required = false) Long bairroId,
            @RequestParam(required = false) StatusDispositivo status,
            @ParameterObject @PageableDefault(size = 20, sort = "instaladoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(UsuarioAutenticado.de(auth), bairroId, status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um aparelho")
    public DispositivoResponse buscar(Authentication auth, @PathVariable Long id) {
        return service.buscar(UsuarioAutenticado.de(auth), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra aparelho")
    public DispositivoResponse criar(Authentication auth, @Valid @RequestBody DispositivoCriarRequest req) {
        return service.criar(UsuarioAutenticado.de(auth), req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza aparelho")
    public DispositivoResponse atualizar(Authentication auth, @PathVariable Long id,
            @Valid @RequestBody DispositivoAtualizarRequest req) {
        return service.atualizar(UsuarioAutenticado.de(auth), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove aparelho")
    public void excluir(Authentication auth, @PathVariable Long id) {
        service.excluir(UsuarioAutenticado.de(auth), id);
    }
}
