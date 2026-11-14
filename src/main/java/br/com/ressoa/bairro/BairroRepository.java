package br.com.ressoa.bairro;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BairroRepository extends JpaRepository<Bairro, Long> {

    @Query("""
            select b from Bairro b
            where (:nome is null or lower(b.nome) like lower(concat('%', :nome, '%')))
              and (:municipio is null or lower(b.municipio) like lower(concat('%', :municipio, '%')))
              and (:uf is null or b.uf = :uf)
              and (:nivel is null or b.nivelRisco = :nivel)
            """)
    Page<Bairro> buscar(@Param("nome") String nome, @Param("municipio") String municipio,
            @Param("uf") String uf, @Param("nivel") NivelRisco nivel, Pageable pageable);

    boolean existsByNomeIgnoreCaseAndCodigoIbge(String nome, String codigoIbge);
}
