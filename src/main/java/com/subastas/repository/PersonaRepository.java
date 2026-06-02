package com.subastas.repository;

import com.subastas.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonaRepository extends JpaRepository<Persona, Integer> {
    boolean existsByDocumento(String documento);
}
