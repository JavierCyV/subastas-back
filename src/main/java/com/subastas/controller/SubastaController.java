package com.subastas.controller;

import com.subastas.repository.CatalogoRepository;
import com.subastas.repository.ItemCatalogoRepository;
import com.subastas.repository.SubastaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subastas")
@RequiredArgsConstructor
public class SubastaController {

    private final SubastaRepository subastaRepo;
    private final CatalogoRepository catalogoRepo;
    private final ItemCatalogoRepository itemCatalogoRepo;

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String categoria) {
        var hoy = java.time.LocalDate.now();
        var estados = List.of("abierta", "cerrada");
        var categoriaNormalizada = categoria != null ? categoria.trim() : null;
        var subastas = (categoriaNormalizada == null || categoriaNormalizada.isBlank() || "todas".equalsIgnoreCase(categoriaNormalizada))
                ? subastaRepo.findByEstadoIn(estados)
                : subastaRepo.findByEstadoInAndCategoriaIgnoreCase(estados, categoriaNormalizada);

        var result = subastas.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",        s.getIdentificador());
            map.put("categoria", s.getCategoria());
            map.put("estado",    s.getEstado());
            map.put("fecha",     s.getFecha() != null ? s.getFecha().toString() : "");
            map.put("hora",      s.getHora()  != null ? s.getHora().toString()  : "");
            map.put("ubicacion", s.getUbicacion() != null ? s.getUbicacion() : "");

            // tipo: en_vivo | proxima | cerrada
            String tipo;
            if ("cerrada".equals(s.getEstado())) {
                tipo = "cerrada";
            } else if (s.getFecha() != null && !s.getFecha().isAfter(hoy)) {
                tipo = "en_vivo";
            } else {
                tipo = "proxima";
            }
            map.put("tipo", tipo);

            // Catalogo info
            catalogoRepo.findBySubasta(s.getIdentificador()).ifPresent(cat -> {
                map.put("descripcion", cat.getDescripcion());
                var items = itemCatalogoRepo.findByCatalogo(cat.getIdentificador());
                map.put("totalItems", items.size());
                items.stream()
                    .map(i -> i.getPreciobase())
                    .min(BigDecimal::compareTo)
                    .ifPresent(min -> map.put("precioBaseMinimo", min));
            });

            if (!map.containsKey("descripcion"))    map.put("descripcion",    "");
            if (!map.containsKey("totalItems"))      map.put("totalItems",      0);
            if (!map.containsKey("precioBaseMinimo")) map.put("precioBaseMinimo", BigDecimal.ZERO);

            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/catalogo")
    public ResponseEntity<?> catalogo(@PathVariable Integer id) {
        var cat = catalogoRepo.findBySubasta(id).orElse(null);
        if (cat == null) return ResponseEntity.ok(List.of());

        var items = itemCatalogoRepo.findByCatalogo(cat.getIdentificador()).stream()
            .map(i -> Map.<String, Object>of(
                "id",         i.getIdentificador(),
                "producto",   i.getProducto(),
                "precioBase", i.getPreciobase(),
                "comision",   i.getComision(),
                "subastado",  i.getSubastado() != null ? i.getSubastado() : "no"
            ))
            .toList();

        return ResponseEntity.ok(Map.of(
            "catalogoId",   cat.getIdentificador(),
            "descripcion",  cat.getDescripcion(),
            "items",        items
        ));
    }
}
