package com.subastas.repository;

import com.subastas.entity.Pujo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pujo p WHERE p.item = :itemId AND p.ganador = 'si'")
    Optional<Pujo> findGanadorByItemForUpdate(Integer itemId);

    @Query("SELECT p FROM Pujo p WHERE p.item = :itemId AND p.ganador = 'si'")
    Optional<Pujo> findGanadorByItem(Integer itemId);

    @Query("SELECT p FROM Pujo p WHERE p.item = :itemId ORDER BY p.importe DESC")
    List<Pujo> findByItemOrderByImporteDesc(Integer itemId);

    @Modifying
    @Query("UPDATE Pujo p SET p.ganador = 'no' WHERE p.item = :itemId AND p.ganador = 'si'")
    void desmarcarGanadorByItem(Integer itemId);
}
