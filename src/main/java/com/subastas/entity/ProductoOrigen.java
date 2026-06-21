package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos_origen")
@Getter @Setter @NoArgsConstructor
public class ProductoOrigen {

    @Id
    @Column(name = "producto")
    private Integer producto;

    @Column(name = "tipo_documento", length = 100)
    private String tipoDocumento;

    @Column(length = 500)
    private String archivo;

    @Column(length = 2)
    private String verificado = "no";
}
