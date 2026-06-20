package com.subastas.controller;

import com.subastas.repository.AsistenteRepository;
import com.subastas.repository.CatalogoRepository;
import com.subastas.repository.MetodoPagoRepository;
import com.subastas.repository.MultaRepository;
import com.subastas.repository.RegistroDeSubastaRepository;
import com.subastas.repository.SubastaRepository;
import com.subastas.repository.UsuarioRepository;
import com.subastas.repository.SolicitudItemRepository;
import com.subastas.repository.VictoriaPagoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
public class PerfilController {

    private final UsuarioRepository usuarioRepo;
    private final AsistenteRepository asistenteRepo;
    private final RegistroDeSubastaRepository registroRepo;
    private final SubastaRepository subastaRepo;
    private final CatalogoRepository catalogoRepo;
    private final SolicitudItemRepository solicitudRepo;
    private final MultaRepository multaRepo;
    private final VictoriaPagoRepository victoriaRepo;
    private final MetodoPagoRepository metodoPagoRepo;

    @GetMapping("/stats")
    public ResponseEntity<?> stats(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        long subastas    = asistenteRepo.countByCliente(userId);
        long ganadas     = registroRepo.countByCliente(userId);
        BigDecimal total = registroRepo.sumImporteByCliente(userId);

        return ResponseEntity.ok(Map.of(
                "subastas",     subastas,
                "ganadas",      ganadas,
                "totalGastado", total
        ));
    }

    @GetMapping("/historial")
    public ResponseEntity<?> historial(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();

        var misGanadas = registroRepo.findByCliente(userId);
        Set<Integer> subastasGanadasIds = new java.util.HashSet<>();

        for (var reg : misGanadas) {
            subastasGanadasIds.add(reg.getSubasta());
            Map<String, Object> item = new HashMap<>();
            item.put("id",       "ganada-" + reg.getIdentificador());
            item.put("subastaId", reg.getSubasta());
            item.put("estado",   "GANADA");
            item.put("importe",  reg.getImporte() != null ? reg.getImporte() : BigDecimal.ZERO);

            String descItem = "Artículo subastado";
            if (reg.getProducto() != null) {
                var prod = solicitudRepo.findById(reg.getProducto()).orElse(null);
                if (prod != null && prod.getTitulo() != null) descItem = prod.getTitulo();
            }
            item.put("descripcion", descItem);

            subastaRepo.findById(reg.getSubasta()).ifPresent(s -> {
                item.put("fecha",     s.getFecha() != null ? s.getFecha().toString() : "");
                item.put("categoria", s.getCategoria() != null ? s.getCategoria() : "");
            });

            item.putIfAbsent("fecha",     "");
            item.putIfAbsent("categoria", "");
            result.add(item);
        }

        var participaciones = asistenteRepo.findByCliente(userId);
        for (var a : participaciones) {
            Integer subastaId = a.getSubasta();
            if (subastasGanadasIds.contains(subastaId)) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("id",       "participo-" + subastaId);
            item.put("subastaId", subastaId);
            item.put("estado",   "PARTICIPÓ");
            item.put("importe",  BigDecimal.ZERO);

            subastaRepo.findById(subastaId).ifPresent(s -> {
                item.put("fecha",     s.getFecha() != null ? s.getFecha().toString() : "");
                item.put("categoria", s.getCategoria() != null ? s.getCategoria() : "");
            });

            catalogoRepo.findBySubasta(subastaId).ifPresent(c ->
                item.put("descripcion", c.getDescripcion())
            );

            item.putIfAbsent("descripcion", "Subasta #" + subastaId);
            item.putIfAbsent("fecha",       "");
            item.putIfAbsent("categoria",   "");
            result.add(item);
        }

        result.sort((a, b) -> String.valueOf(b.get("fecha")).compareTo(String.valueOf(a.get("fecha"))));
        return ResponseEntity.ok(result);
    }

    // ── MULTAS ──────────────────────────────────────────────────────────────────

    @GetMapping("/multas")
    public ResponseEntity<?> multas(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var multas = multaRepo.findByCliente(userId).stream()
                .map(m -> {
                    String titulo = "Artículo";
                    if (m.getRegistro() != null) {
                        var reg = registroRepo.findById(m.getRegistro()).orElse(null);
                        if (reg != null && reg.getProducto() != null) {
                            var prod = solicitudRepo.findById(reg.getProducto()).orElse(null);
                            if (prod != null && prod.getTitulo() != null) titulo = prod.getTitulo();
                        }
                    }
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", m.getIdentificador());
                    map.put("motivo", m.getMotivo() != null ? m.getMotivo() : "");
                    map.put("titulo", titulo);
                    map.put("importe", m.getImporte());
                    map.put("fechamulta", m.getFechamulta() != null ? m.getFechamulta().toString() : "");
                    map.put("pagada", m.getPagada() != null ? m.getPagada() : "no");
                    return map;
                })
                .toList();

        return ResponseEntity.ok(multas);
    }

    // ── VICTORIAS ───────────────────────────────────────────────────────────────

    @GetMapping("/victorias")
    public ResponseEntity<?> victorias(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var victorias = victoriaRepo.findByClienteAndPagado(userId, "no").stream()
                .map(v -> {
                    String titulo = "Artículo";
                    String categoria = "";
                    if (v.getRegistro() != null) {
                        var reg = registroRepo.findById(v.getRegistro()).orElse(null);
                        if (reg != null) {
                            if (reg.getProducto() != null) {
                                var prod = solicitudRepo.findById(reg.getProducto()).orElse(null);
                                if (prod != null && prod.getTitulo() != null) titulo = prod.getTitulo();
                            }
                            if (reg.getSubasta() != null) {
                                var sub = subastaRepo.findById(reg.getSubasta()).orElse(null);
                                if (sub != null && sub.getCategoria() != null) categoria = sub.getCategoria();
                            }
                        }
                    }

                    long horasRestantes = 0;
                    if (v.getFechavictoria() != null) {
                        var limite = v.getFechavictoria().plusHours(72);
                        horasRestantes = LocalDateTime.now().isBefore(limite)
                                ? ChronoUnit.HOURS.between(LocalDateTime.now(), limite)
                                : 0;
                    }

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", v.getIdentificador());
                    map.put("titulo", titulo);
                    map.put("importe", v.getImporte());
                    map.put("fechavictoria", v.getFechavictoria() != null ? v.getFechavictoria().toString() : "");
                    map.put("horasRestantes", horasRestantes);
                    map.put("categoria", categoria);
                    return map;
                })
                .toList();

        return ResponseEntity.ok(victorias);
    }

    record PagarRequest(Integer metodoPagoId) {}

    @PostMapping("/victorias/{id}/pagar")
    public ResponseEntity<?> pagarVictoria(@PathVariable Integer id,
                                           @RequestBody PagarRequest req,
                                           Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var victoria = victoriaRepo.findById(id).orElse(null);
        if (victoria == null) return ResponseEntity.status(404).body(Map.of("error", "Victoria no encontrada"));

        if (!victoria.getCliente().equals(userId))
            return ResponseEntity.status(403).body(Map.of("error", "Esta victoria no te pertenece"));

        if ("si".equals(victoria.getPagado()))
            return ResponseEntity.badRequest().body(Map.of("error", "Esta victoria ya fue pagada"));

        if (req.metodoPagoId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "metodoPagoId es requerido"));

        var metodo = metodoPagoRepo.findById(req.metodoPagoId()).orElse(null);
        if (metodo == null)
            return ResponseEntity.status(404).body(Map.of("error", "Método de pago no encontrado"));

        if (!metodo.getCliente().equals(userId) || !"si".equals(metodo.getActivo()))
            return ResponseEntity.status(403).body(Map.of("error", "El método de pago no te pertenece o no está activo"));

        victoria.setPagado("si");
        victoria.setMetodopago(req.metodoPagoId());
        victoriaRepo.save(victoria);

        return ResponseEntity.ok(Map.of("mensaje", "Victoria pagada correctamente"));
    }
}
