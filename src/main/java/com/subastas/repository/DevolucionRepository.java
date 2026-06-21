package com.subastas.repository;

import com.subastas.entity.Devolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevolucionRepository extends JpaRepository<Devolucion, Integer> {
    List<Devolucion> findByProducto(Integer productoId);
}
