package br.com.ressoa.avaliacao;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ressoa.avaliacao.dto.AvaliacaoDtos.AvaliacaoRequest;
import br.com.ressoa.avaliacao.dto.AvaliacaoDtos.AvaliacaoResponse;
import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.bairro.BairroService;
import br.com.ressoa.bairro.NivelRisco;
import br.com.ressoa.mensageria.RiscoAtualizadoEvent;
import br.com.ressoa.shared.exception.RecursoNaoEncontradoException;
import br.com.ressoa.shared.exception.RegraNegocioException;
import br.com.ressoa.shared.web.PageResponse;

@Service
public class AvaliacaoService {

    private final AvaliacaoRiscoRepository avaliacoes;
    private final BairroService bairroService;
    private final ClassificadorRisco classificador;
    private final ApplicationEventPublisher eventos;

    public AvaliacaoService(AvaliacaoRiscoRepository avaliacoes, BairroService bairroService,
            ClassificadorRisco classificador, ApplicationEventPublisher eventos) {
        this.avaliacoes = avaliacoes;
        this.bairroService = bairroService;
        this.classificador = classificador;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public PageResponse<AvaliacaoResponse> listar(Long bairroId, NivelRisco nivel, Pageable pageable) {
        return PageResponse.of(avaliacoes.buscar(bairroId, nivel, pageable), AvaliacaoResponse::de);
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponse buscar(Long id) {
        return avaliacoes.findById(id).map(AvaliacaoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação", id));
    }

    /**
     * Registra a avaliação, atualiza o nível do bairro e, se o nível mudou, dispara
     * {@code risco.atualizado} (publicado no RabbitMQ após o commit).
     */
    @Transactional
    public AvaliacaoResponse registrar(AvaliacaoRequest req) {
        validarSemana(req.semanaEpidemiologica());
        Bairro bairro = bairroService.obter(req.bairroId());
        NivelRisco nivel = req.nivel() != null ? req.nivel()
                : classificador.classificar(req.casos100k(), req.rt(), req.tempMinMedia());

        AvaliacaoRisco avaliacao = avaliacoes.save(new AvaliacaoRisco(bairro, req.semanaEpidemiologica(), nivel,
                req.tempMinMedia(), req.casos100k(), req.rt()));

        NivelRisco anterior = bairro.getNivelRisco();
        if (anterior != nivel) {
            bairro.alterarNivelRisco(nivel);
            eventos.publishEvent(new RiscoAtualizadoEvent(avaliacao.getId(), bairro.getId(), bairro.getNome(),
                    anterior, nivel, avaliacao.getSemanaEpidemiologica(), Instant.now()));
        }
        return AvaliacaoResponse.de(avaliacao);
    }

    private static void validarSemana(int semana) {
        int numeroSemana = semana % 100;
        if (numeroSemana < 1 || numeroSemana > 53) {
            throw new RegraNegocioException("Semana epidemiológica deve estar entre 01 e 53 (formato AAAASS).");
        }
    }
}
