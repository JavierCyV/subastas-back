package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "personas_dni")
@Getter @Setter @NoArgsConstructor
public class PersonaDni {

    @Id
    @Column(name = "persona_id")
    private Integer personaId;

    @Column(name = "foto_frente", length = 500)
    private String fotoFrente;

    @Column(name = "foto_dorso", length = 500)
    private String fotoDorso;
}
