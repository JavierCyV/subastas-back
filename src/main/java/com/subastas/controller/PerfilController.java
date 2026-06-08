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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
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
}
