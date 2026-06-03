package com.subastas.repository;

import com.subastas.entity.RegistroDeSubasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface RegistroDeSubastaRepository extends JpaRepository<RegistroDeSubasta, Integer> {
    List<RegistroDeSubasta> findByCliente(Integer clienteId);
    long countByCliente(Integer clienteId);

    @Query("SELECT COALESCE(SUM(r.importe), 0) FROM RegistroDeSubasta r WHERE r.cliente = :clienteId")
    BigDecimal sumImporteByCliente(Integer clienteId);
}
