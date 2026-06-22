package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pujos_timestamp")
@Getter @Setter @NoArgsConstructor
public class PujoTimestamp {

    @Id
    @Column(name = "pujo_id")
    private Integer pujoId;

    @Column(name = "fecha_pujo", nullable = false)
    private LocalDateTime fechaPujo = LocalDateTime.now();
}
