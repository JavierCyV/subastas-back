package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "metodospago_verificacion")
@Getter @Setter @NoArgsConstructor
public class MetodoPagoVerificacion {

    @Id
    @Column(name = "metodopago_id")
    private Integer metodoPagoId;

    @Column(nullable = false, length = 2)
    private String verificado = "no";
}
