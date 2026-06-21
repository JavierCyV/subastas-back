package com.subastas.repository;

import com.subastas.entity.MetodoPagoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MetodoPagoVerificacionRepository extends JpaRepository<MetodoPagoVerificacion, Integer> {
    Optional<MetodoPagoVerificacion> findByMetodoPagoId(Integer metodoPagoId);
}
