package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor
public class Usuario {

    @Id
    private Integer identificador;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String rol;  // cliente | duenio | empleado

    @Column(nullable = false, length = 2, columnDefinition = "VARCHAR(2) DEFAULT 'no'")
    private String aprobado = "no";

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @OneToOne
    @JoinColumn(name = "identificador", insertable = false, updatable = false)
    private Persona persona;
}
