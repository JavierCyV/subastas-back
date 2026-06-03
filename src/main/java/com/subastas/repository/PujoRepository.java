package com.subastas.repository;

import com.subastas.entity.Pujo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PujoRepository extends JpaRepository<Pujo, Integer> {

    @Query("""
        SELECT p FROM Pujo p
        WHERE p.item IN (
            SELECT ic.identificador FROM ItemCatalogo ic
            WHERE ic.catalogo IN (
                SELECT c.identificador FROM Catalogo c WHERE c.subasta = :subastaId
            )
        )
        ORDER BY p.importe DESC
    """)
    List<Pujo> findBySubastaId(Integer subastaId);
}
