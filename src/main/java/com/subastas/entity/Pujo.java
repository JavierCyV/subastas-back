package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "pujos")
@Getter @Setter @NoArgsConstructor
public class Pujo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(nullable = false)
    private Integer asistente;

    @Column(nullable = false)
    private Integer item;

    @Column(nullable = false)
    private BigDecimal importe;

    @Column(length = 2)
    private String ganador;
}
