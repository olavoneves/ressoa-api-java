package br.com.ressoa.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByBairroId(Long bairroId);

    List<Usuario> findByBairroIdAndPapel(Long bairroId, Papel papel);
}
