package br.com.ressoa.avaliacao;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.ressoa.bairro.NivelRisco;

public interface AvaliacaoRiscoRepository extends JpaRepository<AvaliacaoRisco, Long> {

    @EntityGraph(attributePaths = "bairro")
    @Query("""
            select a from AvaliacaoRisco a
            where (:bairroId is null or a.bairro.id = :bairroId)
              and (:nivel is null or a.nivel = :nivel)
            """)
    Page<AvaliacaoRisco> buscar(@Param("bairroId") Long bairroId, @Param("nivel") NivelRisco nivel,
            Pageable pageable);

    Optional<AvaliacaoRisco> findFirstByBairroIdOrderByCriadaEmDescIdDesc(Long bairroId);
}
