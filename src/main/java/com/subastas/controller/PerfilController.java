package com.subastas.controller;

import com.subastas.repository.AsistenteRepository;
import com.subastas.repository.CatalogoRepository;
import com.subastas.repository.RegistroDeSubastaRepository;
import com.subastas.repository.SubastaRepository;
import com.subastas.repository.UsuarioRepository;
import com.subastas.repository.SolicitudItemRepository;
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
}
