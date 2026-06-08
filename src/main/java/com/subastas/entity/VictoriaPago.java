package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "victoriaspago")
@Getter @Setter @NoArgsConstructor
public class VictoriaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    private Integer registro;   // registrodesubasta.identificador
    private Integer cliente;
    private BigDecimal importe;
    private LocalDateTime fechavictoria;
    private String pagado = "no";
    private Integer metodopago;
}
