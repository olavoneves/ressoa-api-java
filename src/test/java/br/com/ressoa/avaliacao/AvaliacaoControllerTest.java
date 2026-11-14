package br.com.ressoa.avaliacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import br.com.ressoa.bairro.NivelRisco;
import br.com.ressoa.mensageria.RabbitConfig;
import br.com.ressoa.mensageria.RiscoAtualizadoEvent;
import br.com.ressoa.support.IntegracaoBase;

class AvaliacaoControllerTest extends IntegracaoBase {

    @Test
    @DisplayName("gestor registra avaliação: nível calculado, bairro atualizado e evento publicado")
    void registraEPublica() throws Exception {
        mvc.perform(post("/api/avaliacoes").header(HttpHeaders.AUTHORIZATION, tokenDe("gestor@teste.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("bairroId", bairro.getId(), "semanaEpidemiologica", 202647,
                        "tempMinMedia", 23.4, "casos100k", 48.5, "rt", 1.12))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nivel").value("LARANJA"))
                .andExpect(jsonPath("$.acaoRecomendada").isNotEmpty());

        assertThat(bairros.findById(bairro.getId()).orElseThrow().getNivelRisco()).isEqualTo(NivelRisco.LARANJA);

        ArgumentCaptor<RiscoAtualizadoEvent> evento = ArgumentCaptor.forClass(RiscoAtualizadoEvent.class);
        verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitConfig.EXCHANGE),
                eq(RabbitConfig.ROUTING_KEY_RISCO), evento.capture());
        assertThat(evento.getValue().nivelAnterior()).isEqualTo(NivelRisco.VERDE);
        assertThat(evento.getValue().nivelNovo()).isEqualTo(NivelRisco.LARANJA);
        assertThat(evento.getValue().riscoSubiu()).isTrue();
    }

    @Test
    @DisplayName("avaliação com o mesmo nível não publica evento")
    void mesmoNivelNaoPublica() throws Exception {
        mvc.perform(post("/api/avaliacoes").header(HttpHeaders.AUTHORIZATION, tokenDe("gestor@teste.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("bairroId", bairro.getId(), "semanaEpidemiologica", 202647,
                        "nivel", "VERDE"))))
                .andExpect(status().isCreated());
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    @DisplayName("cidadão não registra avaliação (403), mas consulta o histórico")
    void cidadao() throws Exception {
        String token = tokenDe("maria@teste.com");
        mvc.perform(post("/api/avaliacoes").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("bairroId", bairro.getId(), "semanaEpidemiologica", 202647))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/avaliacoes").param("bairroId", bairro.getId().toString())
                .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("semana epidemiológica inválida devolve 422")
    void semanaInvalida() throws Exception {
        mvc.perform(post("/api/avaliacoes").header(HttpHeaders.AUTHORIZATION, tokenDe("gestor@teste.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("bairroId", bairro.getId(), "semanaEpidemiologica", 202660))))
                .andExpect(status().isUnprocessableContent());
    }
}
