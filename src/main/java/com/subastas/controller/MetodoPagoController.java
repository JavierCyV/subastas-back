package com.subastas.controller;

import com.subastas.entity.MetodoPago;
import com.subastas.repository.MetodoPagoRepository;
import com.subastas.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class MetodoPagoController {

    private static final List<String> TIPOS_VALIDOS = List.of("transferencia", "cheque", "efectivo");

    private final MetodoPagoRepository pagoRepo;
    private final UsuarioRepository usuarioRepo;

    record PagoRequest(@NotBlank String tipo, @NotBlank String detalle) {}

    @GetMapping
    public ResponseEntity<?> listar(Authentication auth) {
        Integer clienteId = getClienteId(auth);
        if (clienteId == null) return ResponseEntity.status(403).build();

        var pagos = pagoRepo.findByClienteAndActivo(clienteId, "si").stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getIdentificador(),
                        "tipo", p.getTipo(),
                        "detalle", p.getDetalle(),
                        "fechaAlta", p.getFechaAlta().toString()
                ))
                .toList();
        return ResponseEntity.ok(pagos);
    }

    @PostMapping
    public ResponseEntity<?> agregar(@Valid @RequestBody PagoRequest req, Authentication auth) {
        Integer clienteId = getClienteId(auth);
        if (clienteId == null) return ResponseEntity.status(403).build();

        if (!TIPOS_VALIDOS.contains(req.tipo())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo inválido. Usar: transferencia, cheque o efectivo"));
        }

        MetodoPago pago = new MetodoPago();
        pago.setCliente(clienteId);
        pago.setTipo(req.tipo());
        pago.setDetalle(req.detalle());
        pago = pagoRepo.save(pago);

        return ResponseEntity.status(201).body(Map.<String, Object>of(
                "id", pago.getIdentificador(),
                "tipo", pago.getTipo(),
                "detalle", pago.getDetalle()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id, Authentication auth) {
        Integer clienteId = getClienteId(auth);
        return pagoRepo.findById(id).map(p -> {
            if (!p.getCliente().equals(clienteId)) {
                return ResponseEntity.status(403).<Object>build();
            }
            p.setActivo("no");
            pagoRepo.save(p);
            return ResponseEntity.<Object>ok(Map.of("mensaje", "Método eliminado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Integer getClienteId(Authentication auth) {
        if (auth == null) return null;
        return usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador())
                .orElse(null);
    }
}
