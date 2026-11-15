package br.com.ressoa.orientacao;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ressoa.shared.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orientação por IA", description = "Spring AI + Gemini: orientação ao morador ou plano de ação ao gestor")
public class OrientacaoController {

    private final OrientacaoService service;

    public OrientacaoController(OrientacaoService service) {
        this.service = service;
    }

    @PostMapping("/api/bairros/{id}/orientacao")
    @Operation(summary = "Gera orientação para o bairro conforme o papel do usuário (CIDADAO → morador; GESTOR → plano de ação)")
    public OrientacaoResponse gerar(Authentication auth, @PathVariable Long id) {
        return service.gerar(id, UsuarioAutenticado.de(auth));
    }
}
