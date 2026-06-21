package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subastas_moneda")
@Getter @Setter @NoArgsConstructor
public class SubastaMoneda {

    @Id
    @Column(name = "subasta_id")
    private Integer subastaId;

    @Column(nullable = false, length = 3)
    private String moneda = "ARS";
}
