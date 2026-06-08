package com.subastas.controller;

import com.subastas.entity.Multa;
import com.subastas.entity.VictoriaPago;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final MetodoPagoRepository metodoPagoRepo;
    private final VictoriaPagoRepository victoriaRepo;

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
                "subastas",    subastas,
                "ganadas",     ganadas,
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

        List<Map<String, Object>> result = new java.util.ArrayList<>();

        // 1. Obtener todas las compras/ventas registradas de este cliente
        var misGanadas = registroRepo.findByCliente(userId);
        Set<Integer> subastasGanadasIds = new java.util.HashSet<>();

        for (var reg : misGanadas) {
            subastasGanadasIds.add(reg.getSubasta());
            Map<String, Object> item = new HashMap<>();
            item.put("id", "ganada-" + reg.getIdentificador());
            item.put("subastaId", reg.getSubasta());
            item.put("estado", "GANADA");
            item.put("importe", reg.getImporte() != null ? reg.getImporte() : BigDecimal.ZERO);

            // Buscar el título del producto específico
            String descItem = "Artículo subastado";
            if (reg.getProducto() != null) {
                var prod = solicitudRepo.findById(reg.getProducto()).orElse(null);
                if (prod != null && prod.getTitulo() != null) {
                    descItem = prod.getTitulo();
                }
            }
            item.put("descripcion", descItem);

            subastaRepo.findById(reg.getSubasta()).ifPresent(s -> {
                item.put("fecha",     s.getFecha() != null ? s.getFecha().toString() : "");
                item.put("categoria", s.getCategoria() != null ? s.getCategoria() : "");
            });

            if (!item.containsKey("fecha"))        item.put("fecha", "");
            if (!item.containsKey("categoria"))    item.put("categoria", "");

            result.add(item);
        }

        // 2. Obtener todas las subastas en las que participó pero no ganó nada
        var participaciones = asistenteRepo.findByCliente(userId);
        for (var a : participaciones) {
            Integer subastaId = a.getSubasta();
            // Si el usuario ya ganó al menos un artículo en esta subasta, no lo duplicamos como participación vacía
            if (subastasGanadasIds.contains(subastaId)) {
                continue;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("id", "participo-" + subastaId);
            item.put("subastaId", subastaId);
            item.put("estado", "PARTICIPÓ");
            item.put("importe", BigDecimal.ZERO);

            subastaRepo.findById(subastaId).ifPresent(s -> {
                item.put("fecha",     s.getFecha() != null ? s.getFecha().toString() : "");
                item.put("categoria", s.getCategoria() != null ? s.getCategoria() : "");
            });

            catalogoRepo.findBySubasta(subastaId).ifPresent(c ->
                item.put("descripcion", c.getDescripcion())
            );

            if (!item.containsKey("descripcion")) item.put("descripcion", "Subasta #" + subastaId);
            if (!item.containsKey("fecha"))        item.put("fecha", "");
            if (!item.containsKey("categoria"))    item.put("categoria", "");

            result.add(item);
        }

        // Ordenar por fecha desc
        result.sort((a, b) -> String.valueOf(b.get("fecha")).compareTo(String.valueOf(a.get("fecha"))));

        return ResponseEntity.ok(result);
    }

    // ── VICTORIAS PENDIENTES DE PAGO ──────────────────────────────────────────

    @GetMapping("/victorias")
    public ResponseEntity<?> victorias(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var pendientes = victoriaRepo.findByClienteAndPagado(userId, "no").stream().map(v -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",      v.getIdentificador());
            m.put("importe", v.getImporte());
            m.put("fechavictoria", v.getFechavictoria() != null ? v.getFechavictoria().toString() : "");

            registroRepo.findById(v.getRegistro()).ifPresent(reg -> {
                if (reg.getProducto() != null) {
                    solicitudRepo.findById(reg.getProducto())
                        .ifPresent(p -> m.put("titulo", p.getTitulo() != null ? p.getTitulo() : "Artículo"));
                }
                subastaRepo.findById(reg.getSubasta())
                    .ifPresent(s -> m.put("categoria", s.getCategoria() != null ? s.getCategoria() : ""));
            });
            if (!m.containsKey("titulo")) m.put("titulo", "Artículo subastado");
            if (!m.containsKey("categoria")) m.put("categoria", "");

            long horasRestantes = 72;
            if (v.getFechavictoria() != null) {
                long horasTranscurridas = java.time.Duration.between(v.getFechavictoria(), LocalDateTime.now()).toHours();
                horasRestantes = Math.max(0, 72 - horasTranscurridas);
            }
            m.put("horasRestantes", horasRestantes);

            return m;
        }).toList();

        return ResponseEntity.ok(pendientes);
    }

    record PagarRequest(Integer metodoPagoId) {}

    @Transactional
    @PostMapping("/victorias/{id}/pagar")
    public ResponseEntity<?> pagar(@PathVariable Integer id, @RequestBody PagarRequest req, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var victoria = victoriaRepo.findById(id).orElse(null);
        if (victoria == null || !victoria.getCliente().equals(userId))
            return ResponseEntity.status(404).body(Map.of("error", "Victoria no encontrada"));
        if ("si".equals(victoria.getPagado()))
            return ResponseEntity.badRequest().body(Map.of("error", "Ya fue pagado"));

        var metodoPago = metodoPagoRepo.findById(req.metodoPagoId()).orElse(null);
        if (metodoPago == null || !metodoPago.getCliente().equals(userId))
            return ResponseEntity.badRequest().body(Map.of("error", "Medio de pago inválido"));

        victoria.setPagado("si");
        victoria.setMetodopago(req.metodoPagoId());
        victoriaRepo.save(victoria);

        return ResponseEntity.ok(Map.of("mensaje", "Pago registrado correctamente"));
    }

    // ── MULTAS ────────────────────────────────────────────────────────────────

    @GetMapping("/multas")
    public ResponseEntity<?> multas(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var result = multaRepo.findByCliente(userId).stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",          m.getIdentificador());
            map.put("importe",     m.getImporte());
            map.put("motivo",      m.getMotivo());
            map.put("pagada",      m.getPagada());
            map.put("fechamulta",  m.getFechamulta() != null ? m.getFechamulta().toString() : "");

            victoriaRepo.findById(m.getRegistro()).ifPresent(v ->
                registroRepo.findById(v.getRegistro()).ifPresent(reg -> {
                    if (reg.getProducto() != null) {
                        solicitudRepo.findById(reg.getProducto())
                            .ifPresent(p -> map.put("titulo", p.getTitulo() != null ? p.getTitulo() : "Artículo"));
                    }
                })
            );
            if (!map.containsKey("titulo")) map.put("titulo", "Artículo subastado");

            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ── SCHEDULER: aplicar multas a los 72h sin pago ──────────────────────────

    @Scheduled(fixedDelay = 3600000) // cada hora
    @Transactional
    public void aplicarMultas() {
        LocalDateTime limite = LocalDateTime.now().minusHours(72);
        victoriaRepo.findByPagadoAndFechavictoriaBefore("no", limite).forEach(v -> {
            if (multaRepo.existsByRegistro(v.getIdentificador())) return;

            BigDecimal importeMulta = v.getImporte()
                .multiply(new BigDecimal("0.10"))
                .setScale(2, java.math.RoundingMode.HALF_UP);

            Multa multa = new Multa();
            multa.setRegistro(v.getIdentificador());
            multa.setCliente(v.getCliente());
            multa.setImporte(importeMulta);
            multa.setMotivo("Incumplimiento de pago en 72 horas");
            multa.setPagada("no");
            multa.setFechamulta(LocalDateTime.now());
            multaRepo.save(multa);
        });
    }
}
