package com.subastas.controller;

import com.subastas.entity.*;
import com.subastas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.HashMap;
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudItemRepository solicitudRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProductoArtistaRepository artistaRepo;
    private final ProductoOrigenRepository origenRepo;
    private final JdbcTemplate jdbcTemplate;

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
        boolean declaracionJurada,
        java.util.List<String> fotosBase64,
        // Item 16: Datos artista/obra
        String artista,
        String fechaObra,
        String historia,
        String dueniosAnteriores,
        // Item 22: Origen lícito
        String tipoDocumentoOrigen,
        String archivoOrigenBase64
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

        java.util.List<String> validFotos = req.fotosBase64() == null ? java.util.List.of() : req.fotosBase64().stream()
                .filter(f -> f != null && !f.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());

        if (validFotos.size() < 5 || validFotos.size() > 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe subir exactamente entre 5 y 6 fotos del artículo."));
        }

        String urls = "";
        if (!validFotos.isEmpty()) {
            java.util.List<String> savedUrls = new java.util.ArrayList<>();
            try {
                java.io.File uploadDir = new java.io.File("uploads");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                for (int i = 0; i < validFotos.size(); i++) {
                    String base64 = validFotos.get(i);
                    String base64Payload = base64;
                    if (base64.contains(",")) {
                        base64Payload = base64.split(",")[1];
                    }
                    byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Payload.trim());
                    String filename = java.util.UUID.randomUUID().toString() + "_" + i + ".jpg";
                    java.nio.file.Path filePath = java.nio.file.Paths.get("uploads", filename);
                    java.nio.file.Files.write(filePath, decodedBytes);
                    savedUrls.add("/uploads/" + filename);
                }
                urls = String.join(",", savedUrls);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Error al guardar las imágenes: " + e.getMessage()));
            }
        }

        SolicitudItem s = new SolicitudItem();
        s.setCliente(userId);
        s.setTitulo(req.titulo().trim());
        s.setCategoria(req.categoria());
        s.setDescripcion(req.descripcion());
        s.setPrecioBaseSugerido(req.precioBaseSugerido());
        s.setArchivoComprobante(urls.isEmpty() ? req.archivoComprobante() : urls);
        s.setDeclaracionJurada(req.declaracionJurada() ? "si" : "no");
        s.setEstado("pendiente");
        s = solicitudRepo.save(s);

        // Item 16: Guardar datos de artista/obra
        if (req.artista() != null && !req.artista().isBlank()) {
            ProductoArtista pa = new ProductoArtista();
            pa.setProducto(s.getIdentificador());
            pa.setArtista(req.artista());
            if (req.fechaObra() != null && !req.fechaObra().isBlank()) {
                try {
                    pa.setFechaObra(LocalDate.parse(req.fechaObra()));
                } catch (Exception ignored) {}
            }
            pa.setHistoria(req.historia());
            pa.setDueniosAnteriores(req.dueniosAnteriores());
            artistaRepo.save(pa);
        }

        // Item 22: Guardar documento de origen lícito
        String archivoOrigenUrl = null;
        if (req.archivoOrigenBase64() != null && !req.archivoOrigenBase64().isBlank()) {
            try {
                String payload = req.archivoOrigenBase64().contains(",") ? req.archivoOrigenBase64().split(",")[1] : req.archivoOrigenBase64();
                byte[] bytes = java.util.Base64.getDecoder().decode(payload.trim());
                String filename = "origen_" + java.util.UUID.randomUUID() + ".pdf";
                java.nio.file.Files.write(java.nio.file.Paths.get("uploads", filename), bytes);
                archivoOrigenUrl = "/uploads/" + filename;
            } catch (Exception ignored) {}
        }
        if ((req.tipoDocumentoOrigen() != null && !req.tipoDocumentoOrigen().isBlank()) || archivoOrigenUrl != null) {
            ProductoOrigen po = new ProductoOrigen();
            po.setProducto(s.getIdentificador());
            po.setTipoDocumento(req.tipoDocumentoOrigen());
            po.setArchivo(archivoOrigenUrl);
            po.setVerificado("no");
            origenRepo.save(po);
        }

        return ResponseEntity.status(201).body(toMap(s));
    }

    // Item 16 + 22: Ver datos extras de una solicitud
    @GetMapping("/{id}/detalle")
    public ResponseEntity<?> detalle(@PathVariable Integer id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        return solicitudRepo.findById(id).map(s -> {
            var result = new HashMap<>(toMap(s));
            artistaRepo.findByProducto(id).ifPresent(a -> {
                result.put("artista", a.getArtista());
                result.put("fechaObra", a.getFechaObra() != null ? a.getFechaObra().toString() : "");
                result.put("historia", a.getHistoria() != null ? a.getHistoria() : "");
                result.put("dueniosAnteriores", a.getDueniosAnteriores() != null ? a.getDueniosAnteriores() : "");
            });
            origenRepo.findByProducto(id).ifPresent(o -> {
                result.put("tipoDocumentoOrigen", o.getTipoDocumento() != null ? o.getTipoDocumento() : "");
                result.put("archivoOrigen", o.getArchivo() != null ? o.getArchivo() : "");
                result.put("origenVerificado", o.getVerificado());
            });
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
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

    @PutMapping("/{id}/seguro/aumentar")
    public ResponseEntity<?> aumentarSeguro(@PathVariable Integer id, @RequestBody Map<String, BigDecimal> body, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        BigDecimal nuevoImporte = body.get("nuevoImporte");
        if (nuevoImporte == null || nuevoImporte.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Importe de póliza inválido"));
        }

        var producto = solicitudRepo.findById(id).orElse(null);
        if (producto == null) return ResponseEntity.notFound().build();
        if (!producto.getCliente().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        // Buscar el número de póliza
        String sqlGetPoliza = "SELECT seguro FROM productos WHERE identificador = ?";
        try {
            String nroPoliza = jdbcTemplate.queryForObject(sqlGetPoliza, String.class, id);
            if (nroPoliza == null || nroPoliza.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Este artículo no posee una póliza de seguro contratada todavía."));
            }

            // Actualizar importe en seguros
            String sqlUpdateSeguro = "UPDATE seguros SET importe = ? WHERE nropoliza = ?";
            int updated = jdbcTemplate.update(sqlUpdateSeguro, nuevoImporte, nroPoliza);
            if (updated == 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "No se encontró el registro de seguro correspondiente."));
            }

            return ResponseEntity.ok(Map.of("mensaje", "Valor de la póliza aumentado correctamente a $" + nuevoImporte));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error al modificar la póliza: " + e.getMessage()));
        }
    }

    private Map<String, Object> toMap(SolicitudItem s) {
        var map = new HashMap<String, Object>();
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
        map.put("archivoComprobante", s.getArchivoComprobante() != null ? s.getArchivoComprobante() : "");

        // Consultar ubicación en depósito si está disponible
        String ubicacion = "No asignada (en espera de recepción)";
        try {
            var listUbi = jdbcTemplate.queryForList(
                "SELECT d.nombre, d.direccion, pu.ubicacion_detalle FROM productos_ubicacion pu JOIN depositos d ON pu.deposito = d.identificador WHERE pu.producto = ?",
                s.getIdentificador()
            );
            if (!listUbi.isEmpty()) {
                var row = listUbi.get(0);
                ubicacion = row.get("nombre") + " — " + row.get("direccion") + " (" + row.get("ubicacion_detalle") + ")";
            }
        } catch (Exception ignored) {}
        map.put("ubicacion", ubicacion);

        // Consultar seguro si está disponible
        Map<String, Object> seguroMap = null;
        try {
            var listSeg = jdbcTemplate.queryForList(
                "SELECT s.nropoliza, s.compania, s.importe FROM productos p JOIN seguros s ON p.seguro = s.nropoliza WHERE p.identificador = ?",
                s.getIdentificador()
            );
            if (!listSeg.isEmpty()) {
                var row = listSeg.get(0);
                seguroMap = new HashMap<>();
                seguroMap.put("nropoliza", row.get("nropoliza"));
                seguroMap.put("compania", row.get("compania"));
                seguroMap.put("importe", row.get("importe"));
            }
        } catch (Exception ignored) {}
        map.put("seguro", seguroMap);

        return map;
    }
}
