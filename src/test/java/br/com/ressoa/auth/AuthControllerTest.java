package br.com.ressoa.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import br.com.ressoa.support.IntegracaoBase;

class AuthControllerTest extends IntegracaoBase {

    @Test
    @DisplayName("registro cria CIDADAO e devolve token")
    void registroCriaCidadao() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("nome", "Ana Souza", "email", "ana@teste.com",
                        "senha", "Senha@123", "bairroId", bairro.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.papel").value("CIDADAO"))
                .andExpect(jsonPath("$.usuario.bairroId").value(bairro.getId()));
    }

    @Test
    @DisplayName("registro com e-mail duplicado devolve 409")
    void registroDuplicado() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("nome", "Maria", "email", "MARIA@teste.com",
                        "senha", "Senha@123", "bairroId", bairro.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito"));
    }

    @Test
    @DisplayName("registro inválido devolve 400 com lista de campos")
    void registroInvalido() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("nome", "A", "email", "invalido", "senha", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.length()").value(5));
    }

    @Test
    @DisplayName("login com senha errada devolve 401")
    void loginSenhaErrada() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("email", "maria@teste.com", "senha", "errada"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("me devolve o perfil do token; sem token devolve 401")
    void perfil() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, tokenDe("gestor@teste.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("GESTOR"));
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }
}
