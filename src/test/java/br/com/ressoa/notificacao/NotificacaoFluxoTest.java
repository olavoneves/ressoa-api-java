package br.com.ressoa.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import br.com.ressoa.avaliacao.AvaliacaoRisco;
import br.com.ressoa.bairro.NivelRisco;
import br.com.ressoa.mensageria.RiscoAtualizadoEvent;
import br.com.ressoa.support.IntegracaoBase;

/** Simula o consumo do evento risco.atualizado (o listener delega para este serviço). */
class NotificacaoFluxoTest extends IntegracaoBase {

    @Autowired
    private RiscoAtualizadoListener listener;

    private RiscoAtualizadoEvent evento(AvaliacaoRisco avaliacao, NivelRisco de, NivelRisco para) {
        return new RiscoAtualizadoEvent(avaliacao.getId(), bairro.getId(), bairro.getNome(), de, para, 202647,
                Instant.now());
    }

    @Test
    @DisplayName("risco subiu: cada morador do bairro recebe um alerta, sem duplicar no reprocessamento")
    void alertaIdempotente() throws Exception {
        AvaliacaoRisco avaliacao = avaliacoes.save(new AvaliacaoRisco(bairro, 202647, NivelRisco.AMARELO, null, null, null));
        listener.aoAtualizarRisco(evento(avaliacao, NivelRisco.VERDE, NivelRisco.AMARELO));
        listener.aoAtualizarRisco(evento(avaliacao, NivelRisco.VERDE, NivelRisco.AMARELO));

        assertThat(notificacoes.count()).isEqualTo(2); // Maria e João (o gestor não recebe)

        String token = tokenDe("maria@teste.com");
        String corpo = mvc.perform(get("/api/notificacoes").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nivel").value("AMARELO"))
                .andExpect(jsonPath("$.content[0].lida").value(false))
                .andReturn().getResponse().getContentAsString();
        long id = lerJson(corpo).get("content").get(0).get("id").asLong();

        mvc.perform(patch("/api/notificacoes/" + id + "/lida").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lida").value(true));
        mvc.perform(get("/api/notificacoes/nao-lidas/contagem").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.naoLidas").value(0));
        mvc.perform(patch("/api/notificacoes/" + id + "/lida")
                .header(HttpHeaders.AUTHORIZATION, tokenDe("joao@teste.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("risco caiu: nenhum alerta")
    void riscoCaiu() {
        AvaliacaoRisco avaliacao = avaliacoes.save(new AvaliacaoRisco(bairro, 202647, NivelRisco.VERDE, null, null, null));
        listener.aoAtualizarRisco(evento(avaliacao, NivelRisco.LARANJA, NivelRisco.VERDE));
        assertThat(notificacoes.count()).isZero();
    }
}
