package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "seguros")
@Getter @Setter @NoArgsConstructor
public class Seguro {

    @Id
    @Column(length = 30)
    private String nropoliza;

    @Column(nullable = false, length = 150)
    private String compania;

    @Column(name = "polizacombinada", length = 2)
    private String polizaCombinada;

    @Column(nullable = false)
    private BigDecimal importe;
}
