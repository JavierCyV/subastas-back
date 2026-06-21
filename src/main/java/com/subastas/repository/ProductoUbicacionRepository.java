package com.subastas.repository;

import com.subastas.entity.ProductoUbicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoUbicacionRepository extends JpaRepository<ProductoUbicacion, Integer> {
    Optional<ProductoUbicacion> findByProducto(Integer productoId);
    List<ProductoUbicacion> findByDeposito(Integer depositoId);
}
