package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas")
@Getter @Setter @NoArgsConstructor
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(nullable = false)
    private Integer registro;

    @Column(nullable = false)
    private Integer cliente;

    @Column(name = "importe_pujado", nullable = false)
    private BigDecimal importePujado;

    @Column(nullable = false)
    private BigDecimal comision;

    @Column(name = "costo_envio")
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "tipo_entrega", length = 20)
    private String tipoEntrega;

    @Column(name = "direccion_envio", length = 250)
    private String direccionEnvio;

    @Column(name = "con_seguro", length = 2)
    private String conSeguro = "si";

    @Column(nullable = false)
    private LocalDateTime emitida = LocalDateTime.now();
}
