package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras_empresa")
@Getter @Setter @NoArgsConstructor
public class CompraEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(nullable = false)
    private Integer subasta;

    @Column(nullable = false)
    private Integer producto;

    @Column(nullable = false)
    private Integer item;

    @Column(nullable = false)
    private BigDecimal importe;

    private LocalDateTime fecha = LocalDateTime.now();
}
