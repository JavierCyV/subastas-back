package com.subastas.repository;

import com.subastas.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    @Query("SELECT c FROM Cliente c WHERE c.admitido = 'no' OR c.admitido IS NULL")
    List<Cliente> findPendientes();
}
