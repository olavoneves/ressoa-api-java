package br.com.ressoa.dispositivo;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ressoa.bairro.Bairro;
import br.com.ressoa.bairro.BairroService;
import br.com.ressoa.dispositivo.dto.DispositivoDtos.DispositivoAtualizarRequest;
import br.com.ressoa.dispositivo.dto.DispositivoDtos.DispositivoCriarRequest;
import br.com.ressoa.dispositivo.dto.DispositivoDtos.DispositivoResponse;
import br.com.ressoa.shared.exception.AcessoNegadoException;
import br.com.ressoa.shared.exception.ConflitoException;
import br.com.ressoa.shared.exception.RecursoNaoEncontradoException;
import br.com.ressoa.shared.exception.RegraNegocioException;
import br.com.ressoa.shared.security.UsuarioAutenticado;
import br.com.ressoa.shared.web.PageResponse;
import br.com.ressoa.usuario.Usuario;
import br.com.ressoa.usuario.UsuarioRepository;

/**
 * Regras de acesso: CIDADAO só enxerga e altera os próprios aparelhos; GESTOR vê e altera todos.
 */
@Service
public class DispositivoService {

    private static final int PULSOS_PADRAO = 1;

    private final DispositivoRepository dispositivos;
    private final UsuarioRepository usuarios;
    private final BairroService bairroService;

    public DispositivoService(DispositivoRepository dispositivos, UsuarioRepository usuarios,
            BairroService bairroService) {
        this.dispositivos = dispositivos;
        this.usuarios = usuarios;
        this.bairroService = bairroService;
    }

    @Transactional(readOnly = true)
    public PageResponse<DispositivoResponse> listar(UsuarioAutenticado atual, Long bairroId, StatusDispositivo status,
            Pageable pageable) {
        Long dono = atual.isGestor() ? null : atual.id();
        return PageResponse.of(dispositivos.buscar(dono, bairroId, status, pageable), DispositivoResponse::de);
    }

    @Transactional(readOnly = true)
    public DispositivoResponse buscar(UsuarioAutenticado atual, Long id) {
        return DispositivoResponse.de(obterComAcesso(atual, id));
    }

    @Transactional
    public DispositivoResponse criar(UsuarioAutenticado atual, DispositivoCriarRequest req) {
        if (dispositivos.existsByNumeroSerieIgnoreCase(req.numeroSerie().trim())) {
            throw new ConflitoException("Já existe um aparelho com este número de série.");
        }
        Usuario dono = resolverDono(atual, req.usuarioId());
        Bairro bairro = resolverBairro(dono, req.bairroId());
        int pulsos = req.pulsosPorDia() == null ? PULSOS_PADRAO : req.pulsosPorDia();
        Dispositivo novo = new Dispositivo(req.numeroSerie(), dono, bairro, req.tipoRecipiente(), req.volumeLitros(),
                pulsos);
        return DispositivoResponse.de(dispositivos.save(novo));
    }

    @Transactional
    public DispositivoResponse atualizar(UsuarioAutenticado atual, Long id, DispositivoAtualizarRequest req) {
        Dispositivo dispositivo = obterComAcesso(atual, id);
        if (req.status() == StatusDispositivo.SEM_SINAL || req.status() == StatusDispositivo.BATERIA_BAIXA) {
            throw new RegraNegocioException("Os status SEM_SINAL e BATERIA_BAIXA são definidos pela telemetria do aparelho.");
        }
        Bairro bairro = bairroService.obter(req.bairroId());
        dispositivo.atualizar(bairro, req.tipoRecipiente(), req.volumeLitros(), req.pulsosPorDia(), req.status());
        return DispositivoResponse.de(dispositivo);
    }

    @Transactional
    public void excluir(UsuarioAutenticado atual, Long id) {
        dispositivos.delete(obterComAcesso(atual, id));
    }

    private Dispositivo obterComAcesso(UsuarioAutenticado atual, Long id) {
        Dispositivo dispositivo = dispositivos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aparelho", id));
        if (!atual.isGestor() && !dispositivo.pertenceA(atual.id())) {
            // 404 em vez de 403 para não revelar a existência de aparelhos de outros moradores
            throw new RecursoNaoEncontradoException("Aparelho", id);
        }
        return dispositivo;
    }

    private Usuario resolverDono(UsuarioAutenticado atual, Long usuarioIdInformado) {
        if (!atual.isGestor() && usuarioIdInformado != null && !usuarioIdInformado.equals(atual.id())) {
            throw new AcessoNegadoException("Morador só pode cadastrar aparelhos para si mesmo.");
        }
        Long donoId = atual.isGestor() && usuarioIdInformado != null ? usuarioIdInformado : atual.id();
        return usuarios.findById(donoId).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", donoId));
    }

    private Bairro resolverBairro(Usuario dono, Long bairroIdInformado) {
        if (bairroIdInformado != null) {
            return bairroService.obter(bairroIdInformado);
        }
        if (dono.getBairro() == null) {
            throw new RegraNegocioException("Informe o bairro do aparelho.");
        }
        return dono.getBairro();
    }
}
