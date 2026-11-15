package br.com.ressoa.bairro;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import br.com.ressoa.support.IntegracaoBase;

class BairroControllerTest extends IntegracaoBase {

    private Map<String, Object> novoBairro() {
        return Map.of("nome", "Grajaú", "municipio", "São Paulo", "uf", "sp",
                "codigoIbge", "3550308", "aguaIntermitente", true);
    }

    @Test
    @DisplayName("listagem é pública e paginada")
    void listagemPublica() throws Exception {
        mvc.perform(get("/api/bairros").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Jardim Ângela"))
                .andExpect(jsonPath("$.content[0].nivelRisco").value("VERDE"))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GESTOR faz CRUD completo")
    void crudGestor() throws Exception {
        String token = tokenDe("gestor@teste.com");
        String corpo = mvc.perform(post("/api/bairros").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(paraJson(novoBairro())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uf").value("SP"))
                .andReturn().getResponse().getContentAsString();
        long id = lerJson(corpo).get("id").asLong();

        mvc.perform(put("/api/bairros/" + id).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("nome", "Grajaú Novo", "municipio", "São Paulo", "uf", "SP",
                        "codigoIbge", "3550308", "aguaIntermitente", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Grajaú Novo"))
                .andExpect(jsonPath("$.aguaIntermitente").value(false));

        mvc.perform(delete("/api/bairros/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/bairros/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CIDADAO não pode criar bairro (403)")
    void cidadaoNaoCria() throws Exception {
        mvc.perform(post("/api/bairros").header(HttpHeaders.AUTHORIZATION, tokenDe("maria@teste.com"))
                .contentType(MediaType.APPLICATION_JSON).content(paraJson(novoBairro())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("bairro com moradores não pode ser excluído (409)")
    void exclusaoComVinculo() throws Exception {
        mvc.perform(delete("/api/bairros/" + bairro.getId())
                .header(HttpHeaders.AUTHORIZATION, tokenDe("gestor@teste.com")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("código IBGE inválido devolve 400")
    void ibgeInvalido() throws Exception {
        mvc.perform(post("/api/bairros").header(HttpHeaders.AUTHORIZATION, tokenDe("gestor@teste.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("nome", "X Bairro", "municipio", "São Paulo", "uf", "SP",
                        "codigoIbge", "12", "aguaIntermitente", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("codigoIbge"));
    }
}
