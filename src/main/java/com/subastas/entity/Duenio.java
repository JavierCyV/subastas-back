package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "duenios")
@Getter @Setter @NoArgsConstructor
public class Duenio {

    @Id
    private Integer identificador;

    @OneToOne
    @JoinColumn(name = "identificador", insertable = false, updatable = false)
    private Persona persona;

    private Integer numeroPais;

    @Column(length = 2)
    private String verificacionFinanciera;

    @Column(length = 2)
    private String verificacionJudicial;

    private Integer calificacionRiesgo;

    @Column(nullable = false)
    private Integer verificador;
}
