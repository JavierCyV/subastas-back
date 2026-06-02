package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "subastas")
@Getter @Setter @NoArgsConstructor
public class Subasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(length = 10)
    private String estado;  // abierta | cerrada

    private Integer subastador;

    @Column(length = 350)
    private String ubicacion;

    private Integer capacidadAsistentes;

    @Column(length = 2)
    private String tieneDeposito;

    @Column(length = 2)
    private String seguridadPropia;

    @Column(length = 10)
    private String categoria;
}
