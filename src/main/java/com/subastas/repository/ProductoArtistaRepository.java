package com.subastas.repository;

import com.subastas.entity.ProductoArtista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductoArtistaRepository extends JpaRepository<ProductoArtista, Integer> {
    Optional<ProductoArtista> findByProducto(Integer productoId);
}
