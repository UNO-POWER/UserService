package com.unopower.userservice.repository;

import com.unopower.userservice.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Login dual: el mismo valor se compara contra username y contra email. */
    Optional<Usuario> findByUsernameOrEmail(String username, String email);
}
