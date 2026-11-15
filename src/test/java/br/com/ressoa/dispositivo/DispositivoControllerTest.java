package br.com.ressoa.dispositivo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import br.com.ressoa.support.IntegracaoBase;

class DispositivoControllerTest extends IntegracaoBase {

    private long criar(String token, String serie) throws Exception {
        String corpo = mvc.perform(post("/api/dispositivos").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("numeroSerie", serie, "tipoRecipiente", "TAMBOR", "volumeLitros", 200))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return lerJson(corpo).get("id").asLong();
    }

    @Test
    @DisplayName("morador cadastra aparelho no próprio bairro com 1 pulso padrão")
    void moradorCadastra() throws Exception {
        String token = tokenDe("maria@teste.com");
        long id = criar(token, "rsa-0001");
        mvc.perform(get("/api/dispositivos/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroSerie").value("RSA-0001"))
                .andExpect(jsonPath("$.pulsosPorDia").value(1))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andExpect(jsonPath("$.bairroId").value(bairro.getId()))
                .andExpect(jsonPath("$.usuarioId").value(morador.getId()));
    }

    @Test
    @DisplayName("morador só vê os próprios aparelhos; gestor vê todos")
    void isolamentoPorDono() throws Exception {
        String maria = tokenDe("maria@teste.com");
        String joao = tokenDe("joao@teste.com");
        long idMaria = criar(maria, "RSA-0001");
        criar(joao, "RSA-0002");

        mvc.perform(get("/api/dispositivos").header(HttpHeaders.AUTHORIZATION, maria))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/dispositivos").header(HttpHeaders.AUTHORIZATION, tokenDe("gestor@teste.com")))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/dispositivos/" + idMaria).header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/dispositivos/" + idMaria).header(HttpHeaders.AUTHORIZATION, joao))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("atualiza pulsos e status; status de telemetria é rejeitado")
    void atualizacao() throws Exception {
        String token = tokenDe("maria@teste.com");
        long id = criar(token, "RSA-0003");
        Map<String, Object> req = new HashMap<>(Map.of("tipoRecipiente", "CAIXA_DAGUA", "volumeLitros", 500,
                "pulsosPorDia", 3, "bairroId", bairro.getId(), "status", "INATIVO"));
        mvc.perform(put("/api/dispositivos/" + id).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(paraJson(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pulsosPorDia").value(3))
                .andExpect(jsonPath("$.status").value("INATIVO"));

        req.put("status", "BATERIA_BAIXA");
        mvc.perform(put("/api/dispositivos/" + id).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(paraJson(req)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("número de série duplicado devolve 409 e sem token devolve 401")
    void duplicadoESemToken() throws Exception {
        String token = tokenDe("maria@teste.com");
        criar(token, "RSA-0004");
        mvc.perform(post("/api/dispositivos").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("numeroSerie", "rsa-0004", "tipoRecipiente", "BALDE", "volumeLitros", 20))))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/dispositivos")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("morador não pode cadastrar aparelho para outro usuário (403)")
    void moradorNaoCadastraParaOutro() throws Exception {
        mvc.perform(post("/api/dispositivos").header(HttpHeaders.AUTHORIZATION, tokenDe("maria@teste.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("numeroSerie", "RSA-0005", "tipoRecipiente", "BALDE", "volumeLitros", 20,
                        "usuarioId", outroMorador.getId()))))
                .andExpect(status().isForbidden());
    }
}
