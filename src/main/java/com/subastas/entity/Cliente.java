package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "clientes")
@Getter @Setter @NoArgsConstructor
public class Cliente {

    @Id
    private Integer identificador;

    @OneToOne
    @JoinColumn(name = "identificador", insertable = false, updatable = false)
    private Persona persona;

    private Integer numeroPais;

    @Column(length = 2)
    private String admitido;

    @Column(length = 10)
    private String categoria;

    @Column(nullable = false)
    private Integer verificador;
}
