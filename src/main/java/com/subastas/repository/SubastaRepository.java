package com.subastas.repository;

import com.subastas.entity.Subasta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubastaRepository extends JpaRepository<Subasta, Integer> {
    List<Subasta> findByEstado(String estado);
    List<Subasta> findByEstadoIn(List<String> estados);
    List<Subasta> findByEstadoInAndCategoriaIgnoreCase(List<String> estados, String categoria);
}
