package com.subastas.controller;

import com.subastas.entity.SolicitudItem;
import com.subastas.repository.SolicitudItemRepository;
import com.subastas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudItemRepository solicitudRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var items = solicitudRepo.findByClienteOrderByFechaSolicitudDesc(userId).stream()
                .map(this::toMap)
                .toList();

        return ResponseEntity.ok(items);
    }

    record SolicitudRequest(
        String titulo,
        String categoria,
        String descripcion,
        BigDecimal precioBaseSugerido,
        String archivoComprobante,
        boolean declaracionJurada
    ) {}

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody SolicitudRequest req, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        if (req.titulo() == null || req.titulo().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "El título es obligatorio"));
        if (!req.declaracionJurada())
            return ResponseEntity.badRequest().body(Map.of("error", "Debe declarar que el bien le pertenece"));

        SolicitudItem s = new SolicitudItem();
        s.setCliente(userId);
        s.setTitulo(req.titulo().trim());
        s.setCategoria(req.categoria());
        s.setDescripcion(req.descripcion());
        s.setPrecioBaseSugerido(req.precioBaseSugerido());
        s.setArchivoComprobante(req.archivoComprobante());
        s.setDeclaracionJurada(req.declaracionJurada() ? "si" : "no");
        s.setEstado("pendiente");

        solicitudRepo.save(s);

        return ResponseEntity.status(201).body(toMap(s));
    }

    // Usuario acepta el precio base y comisión propuesto por la empresa
    @PutMapping("/{id}/aceptar")
    public ResponseEntity<?> aceptar(@PathVariable Integer id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);

        return solicitudRepo.findById(id).map(s -> {
            if (!s.getCliente().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
            if (!"tasado".equals(s.getEstado()))
                return ResponseEntity.badRequest().body(Map.of("error", "La solicitud no está en estado tasado"));

            s.setEstado("aceptado_usuario");
            solicitudRepo.save(s);
            return ResponseEntity.ok(Map.of("mensaje", "Condiciones aceptadas"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Usuario rechaza el precio base y comisión propuesto por la empresa
    @PutMapping("/{id}/rechazar-condiciones")
    public ResponseEntity<?> rechazarCondiciones(@PathVariable Integer id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);

        return solicitudRepo.findById(id).map(s -> {
            if (!s.getCliente().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
            if (!"tasado".equals(s.getEstado()))
                return ResponseEntity.badRequest().body(Map.of("error", "La solicitud no está en estado tasado"));

            s.setEstado("rechazado_usuario");
            solicitudRepo.save(s);
            return ResponseEntity.ok(Map.of("mensaje", "Condiciones rechazadas. El bien será devuelto."));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(SolicitudItem s) {
        var map = new java.util.HashMap<String, Object>();
        map.put("id", s.getIdentificador());
        map.put("titulo", s.getTitulo());
        map.put("categoria", s.getCategoria() != null ? s.getCategoria() : "");
        map.put("descripcion", s.getDescripcion() != null ? s.getDescripcion() : "");
        map.put("estado", s.getEstado());
        map.put("precioBaseSugerido", s.getPrecioBaseSugerido());
        map.put("precioBaseOficial", s.getPrecioBaseOficial());
        map.put("comision", s.getComision());
        map.put("motivoRechazo", s.getMotivoRechazo() != null ? s.getMotivoRechazo() : "");
        map.put("fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : "");
        return map;
    }
}
