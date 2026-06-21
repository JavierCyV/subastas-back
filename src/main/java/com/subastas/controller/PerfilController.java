package com.subastas.controller;

import com.subastas.repository.AsistenteRepository;
import com.subastas.repository.CatalogoRepository;
import com.subastas.repository.RegistroDeSubastaRepository;
import com.subastas.repository.SubastaRepository;
import com.subastas.repository.UsuarioRepository;
import com.subastas.repository.SolicitudItemRepository;
import com.subastas.repository.MultaRepository;
import com.subastas.repository.VictoriaPagoRepository;
import com.subastas.repository.MetodoPagoRepository;
import com.subastas.entity.VictoriaPago;
import com.subastas.entity.Multa;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
    private final VictoriaPagoRepository victoriaPagoRepo;
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

    // ── MULTAS Y BLOQUEOS ──────────────────────────────────────────────────────

    @GetMapping("/multas")
    public ResponseEntity<?> multas(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var multas = multaRepo.findByCliente(userId).stream()
                .map(m -> Map.of(
                        "id",         m.getIdentificador(),
                        "motivo",     m.getMotivo() != null ? m.getMotivo() : "",
                        "titulo",     "Multa de subasta",
                        "importe",    m.getImporte() != null ? m.getImporte() : BigDecimal.ZERO,
                        "fechamulta", m.getFechamulta() != null ? m.getFechamulta().toString() : "",
                        "pagada",     m.getPagada() != null ? m.getPagada() : "no"
                ))
                .toList();

        return ResponseEntity.ok(multas);
    }

    // ── VICTORIAS PENDIENTES DE PAGO ───────────────────────────────────────────

    @GetMapping("/victorias")
    public ResponseEntity<?> victorias(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var victorias = victoriaPagoRepo.findByClienteAndPagado(userId, "no").stream()
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id",            v.getIdentificador());
                    map.put("importe",       v.getImporte() != null ? v.getImporte() : BigDecimal.ZERO);
                    map.put("fechavictoria", v.getFechavictoria() != null ? v.getFechavictoria().toString() : "");
                    map.put("pagado",        v.getPagado() != null ? v.getPagado() : "no");

                    // Calcular horas restantes (72 horas desde la victoria)
                    long horasRestantes = 72;
                    if (v.getFechavictoria() != null) {
                        var ahora = java.time.LocalDateTime.now();
                        var limite = v.getFechavictoria().plusHours(72);
                        horasRestantes = java.time.Duration.between(ahora, limite).toHours();
                        if (horasRestantes < 0) horasRestantes = 0;
                    }
                    map.put("horasRestantes", horasRestantes);

                    // Obtener título del producto y categoría de la subasta a través del registro
                    String titulo = "Artículo ganado";
                    String categoria = "comun";
                    if (v.getRegistro() != null) {
                        var reg = registroRepo.findById(v.getRegistro()).orElse(null);
                        if (reg != null) {
                            var prod = solicitudRepo.findById(reg.getProducto()).orElse(null);
                            if (prod != null && prod.getTitulo() != null) {
                                titulo = prod.getTitulo();
                            }
                            var sub = subastaRepo.findById(reg.getSubasta()).orElse(null);
                            if (sub != null && sub.getCategoria() != null) {
                                categoria = sub.getCategoria();
                            }
                        }
                    }
                    map.put("titulo",    titulo);
                    map.put("categoria", categoria);
                    return map;
                })
                .toList();

        return ResponseEntity.ok(victorias);
    }

    // ── PAGAR VICTORIA ─────────────────────────────────────────────────────────

    @PostMapping("/victorias/{id}/pagar")
    public ResponseEntity<?> pagarVictoria(@PathVariable Integer id, @RequestBody Map<String, Integer> body, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        Integer metodoPagoId = body.get("metodoPagoId");
        if (metodoPagoId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe proporcionar un método de pago"));
        }

        var vic = victoriaPagoRepo.findById(id).orElse(null);
        if (vic == null) return ResponseEntity.notFound().build();
        if (!vic.getCliente().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
        }

        var metodo = metodoPagoRepo.findById(metodoPagoId).orElse(null);
        if (metodo == null || !metodo.getCliente().equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Método de pago inválido"));
        }

        vic.setPagado("si");
        vic.setMetodopago(metodoPagoId);
        victoriaPagoRepo.save(vic);

        return ResponseEntity.ok(Map.of("mensaje", "Pago procesado correctamente"));
    }
}
