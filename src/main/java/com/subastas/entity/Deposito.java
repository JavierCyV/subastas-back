package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "depositos")
@Getter @Setter @NoArgsConstructor
public class Deposito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 250)
    private String direccion;
}
