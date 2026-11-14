package br.com.ressoa.notificacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    @EntityGraph(attributePaths = { "avaliacao", "avaliacao.bairro" })
    @Query("""
            select n from Notificacao n
            where n.usuario.id = :usuarioId
              and (:lida is null or n.lida = :lida)
            """)
    Page<Notificacao> buscar(@Param("usuarioId") Long usuarioId, @Param("lida") Boolean lida, Pageable pageable);

    boolean existsByUsuarioIdAndAvaliacaoId(Long usuarioId, Long avaliacaoId);

    long countByUsuarioIdAndLidaFalse(Long usuarioId);
}
