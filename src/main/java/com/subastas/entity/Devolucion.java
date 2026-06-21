package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "devoluciones")
@Getter @Setter @NoArgsConstructor
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(nullable = false)
    private Integer producto;

    @Column(nullable = false, length = 1000)
    private String motivo;

    private BigDecimal cargo;

    private LocalDateTime fecha = LocalDateTime.now();

    @Column(length = 20)
    private String estado = "pendiente";
}
