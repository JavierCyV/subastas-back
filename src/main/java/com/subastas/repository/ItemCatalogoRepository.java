package com.subastas.repository;

import com.subastas.entity.ItemCatalogo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ItemCatalogoRepository extends JpaRepository<ItemCatalogo, Integer> {
    @Query("SELECT i FROM ItemCatalogo i WHERE i.catalogo = ?1 ORDER BY i.identificador ASC")
    List<ItemCatalogo> findByCatalogo(Integer catalogoId);
}
