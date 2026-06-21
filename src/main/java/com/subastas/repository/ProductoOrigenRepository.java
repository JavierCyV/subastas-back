package com.subastas.repository;

import com.subastas.entity.ProductoOrigen;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductoOrigenRepository extends JpaRepository<ProductoOrigen, Integer> {
    Optional<ProductoOrigen> findByProducto(Integer productoId);
}
