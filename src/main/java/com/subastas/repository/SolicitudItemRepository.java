package com.subastas.repository;

import com.subastas.entity.SolicitudItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudItemRepository extends JpaRepository<SolicitudItem, Integer> {
    List<SolicitudItem> findByClienteOrderByFechaSolicitudDesc(Integer clienteId);
}
