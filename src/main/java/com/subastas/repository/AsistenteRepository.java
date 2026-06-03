package com.subastas.repository;

import com.subastas.entity.Asistente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsistenteRepository extends JpaRepository<Asistente, Integer> {
    List<Asistente> findByCliente(Integer clienteId);
    long countByCliente(Integer clienteId);
}
