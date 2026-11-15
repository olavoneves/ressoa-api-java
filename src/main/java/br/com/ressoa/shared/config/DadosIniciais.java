package br.com.ressoa.shared.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.bairro.BairroRepository;
import br.com.ressoa.dispositivo.Dispositivo;
import br.com.ressoa.dispositivo.DispositivoRepository;
import br.com.ressoa.dispositivo.TipoRecipiente;
import br.com.ressoa.usuario.Papel;
import br.com.ressoa.usuario.Usuario;
import br.com.ressoa.usuario.UsuarioRepository;

/**
 * Carga inicial para demonstração: só roda com o banco vazio (sem usuários).
 * Cria bairros reais de periferia com abastecimento intermitente, os usuários de teste
 * documentados no README e alguns aparelhos.
 */
@Component
@ConditionalOnProperty(name = "ressoa.seed.habilitado", havingValue = "true")
public class DadosIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private final BairroRepository bairros;
    private final UsuarioRepository usuarios;
    private final DispositivoRepository dispositivos;
    private final PasswordEncoder passwordEncoder;
    private final String senhaPadrao;

    public DadosIniciais(BairroRepository bairros, UsuarioRepository usuarios, DispositivoRepository dispositivos,
            PasswordEncoder passwordEncoder, @Value("${ressoa.seed.senha-padrao}") String senhaPadrao) {
        this.bairros = bairros;
        this.usuarios = usuarios;
        this.dispositivos = dispositivos;
        this.passwordEncoder = passwordEncoder;
        this.senhaPadrao = senhaPadrao;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }
        log.info("Banco vazio: carregando dados iniciais de demonstração");

        List<Bairro> lista = bairros.saveAll(List.of(
                new Bairro("Jardim Ângela", "São Paulo", "SP", "3550308", true),
                new Bairro("Grajaú", "São Paulo", "SP", "3550308", true),
                new Bairro("Complexo da Maré", "Rio de Janeiro", "RJ", "3304557", true),
                new Bairro("Vila Pinho", "Belo Horizonte", "MG", "3106200", true),
                new Bairro("Ibura", "Recife", "PE", "2611606", true),
                new Bairro("Moema", "São Paulo", "SP", "3550308", false)));
        Bairro jardimAngela = lista.get(0);
        Bairro grajau = lista.get(1);

        String hash = passwordEncoder.encode(senhaPadrao);
        usuarios.save(new Usuario("Gestora Vigilância SP", "gestor@ressoa.app", hash, Papel.GESTOR, jardimAngela));
        usuarios.save(new Usuario("Serviço de IA Ressoa", "ia-servico@ressoa.app", hash, Papel.GESTOR, null));
        Usuario maria = usuarios.save(new Usuario("Maria Aparecida", "morador@ressoa.app", hash, Papel.CIDADAO,
                jardimAngela));
        Usuario joao = usuarios.save(new Usuario("João Batista", "joao@ressoa.app", hash, Papel.CIDADAO, grajau));

        dispositivos.saveAll(List.of(
                new Dispositivo("RSA-2026-0001", maria, jardimAngela, TipoRecipiente.TAMBOR, 200, 1),
                new Dispositivo("RSA-2026-0002", maria, jardimAngela, TipoRecipiente.CAIXA_DAGUA, 500, 1),
                new Dispositivo("RSA-2026-0003", joao, grajau, TipoRecipiente.BALDE, 20, 1)));
    }
}
