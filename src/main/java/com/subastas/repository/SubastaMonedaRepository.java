package com.subastas.repository;

import com.subastas.entity.SubastaMoneda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubastaMonedaRepository extends JpaRepository<SubastaMoneda, Integer> {
    Optional<SubastaMoneda> findBySubastaId(Integer subastaId);
}
