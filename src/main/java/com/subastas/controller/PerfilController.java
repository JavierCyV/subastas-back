package com.subastas.controller;

import com.subastas.repository.AsistenteRepository;
import com.subastas.repository.CatalogoRepository;
import com.subastas.repository.RegistroDeSubastaRepository;
import com.subastas.repository.SubastaRepository;
import com.subastas.repository.UsuarioRepository;
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

        // Subastas ganadas por el usuario: mapeadas por subastaId → importe
        Map<Integer, BigDecimal> ganadas = registroRepo.findByCliente(userId).stream()
                .collect(Collectors.toMap(
                        r -> r.getSubasta(),
                        r -> r.getImporte() != null ? r.getImporte() : BigDecimal.ZERO,
                        (a, b) -> a
                ));

        // Subastas en que participó
        Set<Integer> subastaIds = asistenteRepo.findByCliente(userId).stream()
                .map(a -> a.getSubasta())
                .collect(Collectors.toSet());

        List<Map<String, Object>> result = subastaIds.stream().map(subastaId -> {
            Map<String, Object> item = new HashMap<>();
            item.put("subastaId", subastaId);
            item.put("estado", ganadas.containsKey(subastaId) ? "GANADA" : "PARTICIPÓ");
            item.put("importe", ganadas.getOrDefault(subastaId, BigDecimal.ZERO));

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

            return item;
        }).sorted((a, b) -> String.valueOf(b.get("fecha")).compareTo(String.valueOf(a.get("fecha"))))
          .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
