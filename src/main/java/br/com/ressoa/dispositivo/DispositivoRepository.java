package br.com.ressoa.dispositivo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DispositivoRepository extends JpaRepository<Dispositivo, Long> {

    @EntityGraph(attributePaths = { "usuario", "bairro" })
    @Query("""
            select d from Dispositivo d
            where (:usuarioId is null or d.usuario.id = :usuarioId)
              and (:bairroId is null or d.bairro.id = :bairroId)
              and (:status is null or d.status = :status)
            """)
    Page<Dispositivo> buscar(@Param("usuarioId") Long usuarioId, @Param("bairroId") Long bairroId,
            @Param("status") StatusDispositivo status, Pageable pageable);

    boolean existsByNumeroSerieIgnoreCase(String numeroSerie);

    boolean existsByBairroId(Long bairroId);
}
