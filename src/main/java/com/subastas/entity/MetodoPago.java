package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "metodosDePago")
@Getter @Setter @NoArgsConstructor
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(nullable = false)
    private Integer cliente;

    @Column(nullable = false, length = 20)
    private String tipo;  // transferencia | cheque | efectivo

    @Column(nullable = false, length = 500)
    private String detalle;

    @Column(nullable = false, length = 2, columnDefinition = "VARCHAR(2) DEFAULT 'si'")
    private String activo = "si";

    private LocalDateTime fechaAlta = LocalDateTime.now();
}
