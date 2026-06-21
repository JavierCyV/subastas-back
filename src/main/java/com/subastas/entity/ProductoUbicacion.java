package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos_ubicacion")
@Getter @Setter @NoArgsConstructor
public class ProductoUbicacion {

    @Id
    @Column(name = "producto")
    private Integer producto;

    @Column(name = "deposito", nullable = false)
    private Integer deposito;

    @Column(name = "ubicacion_detalle", length = 500)
    private String ubicacionDetalle;
}
