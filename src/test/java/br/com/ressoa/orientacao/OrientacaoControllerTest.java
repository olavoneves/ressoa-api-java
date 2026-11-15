package br.com.ressoa.orientacao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import br.com.ressoa.support.IntegracaoBase;

/** No perfil de teste o LLM está desligado: valida o fluxo e a orientação de contingência. */
class OrientacaoControllerTest extends IntegracaoBase {

    @Test
    @DisplayName("morador recebe orientação simples de contingência quando a IA está indisponível")
    void morador() throws Exception {
        mvc.perform(post("/api/bairros/" + bairro.getId() + "/orientacao")
                .header(HttpHeaders.AUTHORIZATION, tokenDe("maria@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publico").value("MORADOR"))
                .andExpect(jsonPath("$.geradaPorIa").value(false))
                .andExpect(jsonPath("$.texto").value(Matchers.containsString("Ressoa")));
    }

    @Test
    @DisplayName("gestor recebe plano de ação")
    void gestor() throws Exception {
        mvc.perform(post("/api/bairros/" + bairro.getId() + "/orientacao")
                .header(HttpHeaders.AUTHORIZATION, tokenDe("gestor@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publico").value("GESTOR"))
                .andExpect(jsonPath("$.texto").value(Matchers.containsString("Público prioritário")));
    }

    @Test
    @DisplayName("bairro inexistente devolve 404 e sem token devolve 401")
    void erros() throws Exception {
        mvc.perform(post("/api/bairros/999999/orientacao").header(HttpHeaders.AUTHORIZATION, tokenDe("maria@teste.com")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/bairros/" + bairro.getId() + "/orientacao"))
                .andExpect(status().isUnauthorized());
    }
}
