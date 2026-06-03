package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "itemscatalogo")
@Getter @Setter @NoArgsConstructor
public class ItemCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(nullable = false)
    private Integer catalogo;

    @Column(nullable = false)
    private Integer producto;

    @Column(nullable = false)
    private BigDecimal preciobase;

    @Column(nullable = false)
    private BigDecimal comision;

    @Column(length = 2)
    private String subastado;
}
