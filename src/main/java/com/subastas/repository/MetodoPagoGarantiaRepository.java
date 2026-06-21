package com.subastas.repository;

import com.subastas.entity.MetodoPagoGarantia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetodoPagoGarantiaRepository extends JpaRepository<MetodoPagoGarantia, Integer> {
    Optional<MetodoPagoGarantia> findByMetodoPagoId(Integer metodoPagoId);
}
