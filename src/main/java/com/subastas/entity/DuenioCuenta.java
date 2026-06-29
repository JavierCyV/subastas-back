package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "duenios_cuenta")
@Getter @Setter @NoArgsConstructor
public class DuenioCuenta {

    @Id
    @Column(name = "duenio")
    private Integer duenio;

    @Column(length = 200)
    private String banco;

    @Column(name = "tipo_cuenta", length = 50)
    private String tipoCuenta;

    @Column(name = "numero_cuenta", length = 100)
    private String numeroCuenta;

    @Column(length = 3)
    private String moneda = "ARS";

    @Column(name = "es_exterior", length = 2)
    private String esExterior = "no";

    @Column(precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;
}
