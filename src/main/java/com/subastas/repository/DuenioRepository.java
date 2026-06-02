package com.subastas.repository;

import com.subastas.entity.Duenio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DuenioRepository extends JpaRepository<Duenio, Integer> {

    @Query("SELECT d FROM Duenio d WHERE d.verificacionFinanciera IS NULL OR d.verificacionJudicial IS NULL")
    List<Duenio> findPendientes();
}
