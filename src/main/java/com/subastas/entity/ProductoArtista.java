package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "productos_artista")
@Getter @Setter @NoArgsConstructor
public class ProductoArtista {

    @Id
    @Column(name = "producto")
    private Integer producto;

    @Column(length = 200)
    private String artista;

    @Column(name = "fecha_obra")
    private LocalDate fechaObra;

    @Column(columnDefinition = "TEXT")
    private String historia;

    @Column(name = "duenios_anteriores", columnDefinition = "TEXT")
    private String dueniosAnteriores;
}
