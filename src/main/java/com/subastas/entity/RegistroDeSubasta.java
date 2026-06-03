package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "registrodesubasta")
@Getter @Setter @NoArgsConstructor
public class RegistroDeSubasta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    private Integer subasta;
    private Integer duenio;
    private Integer producto;
    private Integer cliente;

    private BigDecimal importe;
    private BigDecimal comision;
}
