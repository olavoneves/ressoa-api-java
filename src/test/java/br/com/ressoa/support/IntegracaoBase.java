package br.com.ressoa.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.ressoa.avaliacao.AvaliacaoRiscoRepository;
import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.bairro.BairroRepository;
import br.com.ressoa.dispositivo.DispositivoRepository;
import br.com.ressoa.notificacao.NotificacaoRepository;
import br.com.ressoa.usuario.Papel;
import br.com.ressoa.usuario.Usuario;
import br.com.ressoa.usuario.UsuarioRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base dos testes de integração: sobe a aplicação com H2 (perfil test), limpa o banco
 * (RabbitMQ substituído por mock) e cria um gestor, dois moradores e um bairro antes de cada teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegracaoBase {

    protected static final String SENHA = "Senha@123";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JsonMapper json;
    @Autowired
    protected BairroRepository bairros;
    @Autowired
    protected UsuarioRepository usuarios;
    @Autowired
    protected DispositivoRepository dispositivos;
    @Autowired
    protected AvaliacaoRiscoRepository avaliacoes;
    @Autowired
    protected NotificacaoRepository notificacoes;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    protected RabbitTemplate rabbitTemplate;

    protected Bairro bairro;
    protected Usuario gestor;
    protected Usuario morador;
    protected Usuario outroMorador;

    @BeforeEach
    void prepararBanco() {
        limparBanco();
        bairro = bairros.save(new Bairro("Jardim Ângela", "São Paulo", "SP", "3550308", true));
        String hash = passwordEncoder.encode(SENHA);
        gestor = usuarios.save(new Usuario("Gestora", "gestor@teste.com", hash, Papel.GESTOR, bairro));
        morador = usuarios.save(new Usuario("Maria", "maria@teste.com", hash, Papel.CIDADAO, bairro));
        outroMorador = usuarios.save(new Usuario("João", "joao@teste.com", hash, Papel.CIDADAO, bairro));
    }

    /** Ponto de extensão para subclasses limparem tabelas adicionais antes das básicas. */
    protected void limparBanco() {
        notificacoes.deleteAll();
        avaliacoes.deleteAll();
        dispositivos.deleteAll();
        usuarios.deleteAll();
        bairros.deleteAll();
    }

    protected String tokenDe(String email) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "senha", SENHA))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(corpo).get("token").asString();
    }

    protected String paraJson(Object valor) {
        return json.writeValueAsString(valor);
    }

    protected JsonNode lerJson(String corpo) {
        return json.readTree(corpo);
    }
}
