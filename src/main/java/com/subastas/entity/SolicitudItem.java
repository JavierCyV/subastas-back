package com.subastas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "productos")
@Getter @Setter @NoArgsConstructor
public class SolicitudItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer identificador;

    // Cliente que hace la solicitud
    @Column(name = "clientesolicitante")
    private Integer cliente;

    @Column(length = 200)
    private String titulo;

    // Categoría del bien (Arte, Relojes, Vehículos, Otros)
    @Column(name = "descripcioncatalogo", length = 500)
    private String categoria;

    @Column(name = "descripcioncompleta", length = 300)
    private String descripcion;

    @Column(name = "preciobasesugerido")
    private BigDecimal precioBaseSugerido;

    // Estado del flujo: pendiente | inspeccion | tasado |
    //   aceptado_empresa | rechazado_empresa | aceptado_usuario | rechazado_usuario
    @Column(length = 25)
    private String estado = "pendiente";

    // Tasación del admin
    @Column(name = "preciobaseoficial")
    private BigDecimal precioBaseOficial;

    @Column(name = "comisionoficial")
    private BigDecimal comision;

    @Column(name = "motivorechazo", length = 1000)
    private String motivoRechazo;

    @Column(name = "archivocomprobante", length = 500)
    private String archivoComprobante;

    @Column(name = "declaracionjurada", length = 2)
    private String declaracionJurada = "no";

    @Column(name = "fechasolicitud")
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    // Campos heredados de productos (opcionales)
    private LocalDate fecha;

    @Column(length = 2)
    private String disponible = "no";

    // revisor e duenio se asignan cuando el admin acepta el bien
    private Integer revisor;
    private Integer duenio;
}
