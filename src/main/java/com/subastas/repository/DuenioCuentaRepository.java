package com.subastas.repository;

import com.subastas.entity.DuenioCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DuenioCuentaRepository extends JpaRepository<DuenioCuenta, Integer> {
    Optional<DuenioCuenta> findByDuenio(Integer duenioId);
}
