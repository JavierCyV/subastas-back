package com.subastas.repository;

import com.subastas.entity.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Integer> {
    List<MetodoPago> findByClienteAndActivo(Integer cliente, String activo);
}
