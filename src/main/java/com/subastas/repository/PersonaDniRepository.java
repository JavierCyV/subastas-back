package com.subastas.repository;

import com.subastas.entity.PersonaDni;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonaDniRepository extends JpaRepository<PersonaDni, Integer> {
    Optional<PersonaDni> findByPersonaId(Integer personaId);
}
