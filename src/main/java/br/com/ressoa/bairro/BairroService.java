package br.com.ressoa.bairro;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.ressoa.bairro.dto.BairroDtos.BairroRequest;
import br.com.ressoa.bairro.dto.BairroDtos.BairroResponse;
import br.com.ressoa.dispositivo.DispositivoRepository;
import br.com.ressoa.shared.exception.ConflitoException;
import br.com.ressoa.shared.exception.RecursoNaoEncontradoException;
import br.com.ressoa.shared.web.PageResponse;
import br.com.ressoa.usuario.UsuarioRepository;

@Service
public class BairroService {

    private final BairroRepository bairros;
    private final UsuarioRepository usuarios;
    private final DispositivoRepository dispositivos;

    public BairroService(BairroRepository bairros, UsuarioRepository usuarios, DispositivoRepository dispositivos) {
        this.bairros = bairros;
        this.usuarios = usuarios;
        this.dispositivos = dispositivos;
    }

    @Transactional(readOnly = true)
    public PageResponse<BairroResponse> listar(String nome, String municipio, String uf, NivelRisco nivel,
            Pageable pageable) {
        return PageResponse.of(bairros.buscar(vazioParaNulo(nome), vazioParaNulo(municipio),
                uf == null || uf.isBlank() ? null : uf.toUpperCase(), nivel, pageable), BairroResponse::de);
    }

    @Transactional(readOnly = true)
    public BairroResponse buscar(Long id) {
        return BairroResponse.de(obter(id));
    }

    @Transactional
    public BairroResponse criar(BairroRequest req) {
        if (bairros.existsByNomeIgnoreCaseAndCodigoIbge(req.nome().trim(), req.codigoIbge().trim())) {
            throw new ConflitoException("Já existe um bairro com este nome neste município.");
        }
        Bairro bairro = new Bairro(req.nome(), req.municipio(), req.uf(), req.codigoIbge(), req.aguaIntermitente());
        return BairroResponse.de(bairros.save(bairro));
    }

    @Transactional
    public BairroResponse atualizar(Long id, BairroRequest req) {
        Bairro bairro = obter(id);
        bairro.atualizar(req.nome(), req.municipio(), req.uf(), req.codigoIbge(), req.aguaIntermitente());
        return BairroResponse.de(bairro);
    }

    @Transactional
    public void excluir(Long id) {
        Bairro bairro = obter(id);
        if (usuarios.existsByBairroId(id) || dispositivos.existsByBairroId(id)) {
            throw new ConflitoException("Bairro possui moradores ou aparelhos vinculados e não pode ser excluído.");
        }
        bairros.delete(bairro);
    }

    public Bairro obter(Long id) {
        return bairros.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Bairro", id));
    }

    private static String vazioParaNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
