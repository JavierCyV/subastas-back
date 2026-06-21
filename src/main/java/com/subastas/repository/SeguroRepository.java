package com.subastas.repository;

import com.subastas.entity.Seguro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeguroRepository extends JpaRepository<Seguro, String> {
}
