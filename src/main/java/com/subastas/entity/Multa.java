package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "multas")
@Getter @Setter @NoArgsConstructor
public class Multa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    private Integer registro;
    private Integer cliente;
    private BigDecimal importe;
    private String motivo;
    private String pagada = "no";
    private LocalDateTime fechamulta;
}
