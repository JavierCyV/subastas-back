package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "empleados")
@Getter @Setter @NoArgsConstructor
public class Empleado {

    @Id
    private Integer identificador;

    @OneToOne
    @JoinColumn(name = "identificador", insertable = false, updatable = false)
    private Persona persona;

    @Column(length = 100)
    private String cargo;

    private Integer sector;
}
