package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "metodospago_garantia")
@Getter @Setter @NoArgsConstructor
public class MetodoPagoGarantia {

    @Id
    @Column(name = "metodopago_id")
    private Integer metodoPagoId;

    @Column(nullable = false)
    private BigDecimal montoGarantia;
}
