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

@RestController
@RequestMapping("/api/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudItemRepository solicitudRepo;
    private final UsuarioRepository usuarioRepo;
    private final ProductoArtistaRepository artistaRepo;
    private final ProductoOrigenRepository origenRepo;

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
        return map;
    }
}
