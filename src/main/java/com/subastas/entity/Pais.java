package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "paises")
@Getter @Setter @NoArgsConstructor
public class Pais {

    @Id
    private Integer numero;

    @Column(nullable = false, length = 250)
    private String nombre;

    @Column(length = 250)
    private String nombreCorto;

    @Column(nullable = false, length = 250)
    private String capital;

    @Column(nullable = false, length = 250)
    private String nacionalidad;

    @Column(nullable = false, length = 150)
    private String idiomas;
}
