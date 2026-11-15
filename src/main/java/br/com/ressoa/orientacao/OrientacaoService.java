package br.com.ressoa.orientacao;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import br.com.ressoa.avaliacao.AvaliacaoRisco;
import br.com.ressoa.avaliacao.AvaliacaoRiscoRepository;
import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.bairro.BairroService;
import br.com.ressoa.orientacao.OrientacaoResponse.Publico;
import br.com.ressoa.shared.security.UsuarioAutenticado;

/**
 * Funcionalidade de IA generativa integrada ao fluxo (Spring AI + Gemini):
 * a partir do nível de risco e dos indicadores do bairro, gera uma orientação simples para o
 * morador ou um plano de ação para o gestor.
 *
 * <p>A orientação do morador é gravada em {@code AVALIACAO_RISCO.ORIENTACAO_IA} e reaproveitada
 * enquanto não houver nova avaliação (economia de tokens). Sem LLM, usa o protocolo de contingência.
 * A chamada ao LLM fica fora de transação para não prender conexão com o banco.
 */
@Service
public class OrientacaoService {

    private static final Logger log = LoggerFactory.getLogger(OrientacaoService.class);

    private final BairroService bairroService;
    private final AvaliacaoRiscoRepository avaliacoes;
    private final ChatClient chatClient;

    public OrientacaoService(BairroService bairroService, AvaliacaoRiscoRepository avaliacoes,
            ObjectProvider<ChatModel> chatModel) {
        this.bairroService = bairroService;
        this.avaliacoes = avaliacoes;
        ChatModel modelo = chatModel.getIfAvailable();
        this.chatClient = modelo == null ? null : ChatClient.create(modelo);
        if (modelo == null) {
            log.warn("Nenhum modelo de IA configurado: orientações usarão o protocolo de contingência");
        }
    }

    public OrientacaoResponse gerar(Long bairroId, UsuarioAutenticado usuario) {
        Bairro bairro = bairroService.obter(bairroId);
        Optional<AvaliacaoRisco> ultima = avaliacoes.findFirstByBairroIdOrderByCriadaEmDescIdDesc(bairroId);
        return usuario.isGestor() ? planoGestor(bairro, ultima.orElse(null))
                : orientacaoMorador(bairro, ultima.orElse(null));
    }

    private OrientacaoResponse orientacaoMorador(Bairro bairro, AvaliacaoRisco avaliacao) {
        if (avaliacao != null && avaliacao.getOrientacaoIa() != null && avaliacao.getNivel() == bairro.getNivelRisco()) {
            return resposta(bairro, Publico.MORADOR, avaliacao.getOrientacaoIa(), true);
        }
        Optional<String> texto = chamarLlm(PromptsOrientacao.SISTEMA_MORADOR,
                "Gere a orientação para o morador com base nestes dados:\n" + PromptsOrientacao.contexto(bairro, avaliacao));
        if (texto.isPresent() && avaliacao != null) {
            avaliacao.registrarOrientacao(texto.get());
            avaliacoes.save(avaliacao);
        }
        return resposta(bairro, Publico.MORADOR,
                texto.orElseGet(() -> OrientacaoContingencia.morador(bairro.getNivelRisco())), texto.isPresent());
    }

    private OrientacaoResponse planoGestor(Bairro bairro, AvaliacaoRisco avaliacao) {
        Optional<String> texto = chamarLlm(PromptsOrientacao.SISTEMA_GESTOR,
                "Gere o plano de ação com base nestes dados:\n" + PromptsOrientacao.contexto(bairro, avaliacao));
        return resposta(bairro, Publico.GESTOR, texto.orElseGet(() -> OrientacaoContingencia.gestor(bairro)),
                texto.isPresent());
    }

    private Optional<String> chamarLlm(String sistema, String usuario) {
        if (chatClient == null) {
            return Optional.empty();
        }
        try {
            String conteudo = chatClient.prompt().system(sistema).user(usuario).call().content();
            return Optional.ofNullable(conteudo).map(String::strip).filter(s -> !s.isEmpty());
        } catch (RuntimeException ex) {
            log.error("Falha ao gerar orientação com IA; usando contingência: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static OrientacaoResponse resposta(Bairro bairro, Publico publico, String texto, boolean geradaPorIa) {
        return new OrientacaoResponse(bairro.getId(), bairro.getNome(), bairro.getNivelRisco(), publico, texto,
                geradaPorIa, Instant.now());
    }
}
