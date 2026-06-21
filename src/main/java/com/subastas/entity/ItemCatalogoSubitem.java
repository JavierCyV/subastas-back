package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "itemscatalogo_subitems")
@Getter @Setter @NoArgsConstructor
public class ItemCatalogoSubitem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    @Column(name = "item_catalogo", nullable = false)
    private Integer itemCatalogo;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(nullable = false)
    private Integer cantidad = 1;
}
