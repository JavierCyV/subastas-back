package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "registros_pendientes")
@Getter @Setter @NoArgsConstructor
public class RegistroPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String documento;

    @Column(length = 250)
    private String direccion;

    @Column(length = 50)
    private String telefono;

    private Integer pais;

    @Column(name = "foto_dni_frente", length = 500)
    private String fotoDniFrente;

    @Column(name = "foto_dni_dorso", length = 500)
    private String fotoDniDorso;

    @Column(nullable = false, length = 20)
    private String rol;

    @Column(name = "codigo_completar", length = 6)
    private String codigoCompletar;

    @Column(name = "codigo_expiracion")
    private LocalDateTime codigoExpiracion;

    @Column(nullable = false)
    private LocalDateTime creado = LocalDateTime.now();
}
