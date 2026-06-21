package com.subastas.repository;

import com.subastas.entity.RegistroPendiente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistroPendienteRepository extends JpaRepository<RegistroPendiente, Integer> {
    Optional<RegistroPendiente> findByEmail(String email);
    Optional<RegistroPendiente> findByCodigoCompletar(String codigo);
    List<RegistroPendiente> findByCodigoCompletarIsNull();
}
