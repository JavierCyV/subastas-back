package com.subastas.repository;

import com.subastas.entity.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    List<Factura> findByCliente(Integer clienteId);
}
