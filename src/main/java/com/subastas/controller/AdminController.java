package com.subastas.controller;

import com.subastas.entity.*;
import com.subastas.repository.*;
import com.subastas.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('EMPLEADO')")
@RequiredArgsConstructor
public class AdminController {

    private final UsuarioRepository usuarioRepo;
    private final ClienteRepository clienteRepo;
    private final DuenioRepository duenioRepo;
    private final SubastaRepository subastaRepo;
    private final JavaMailSender mailSender;

    // ── USUARIOS PENDIENTES ────────────────────────────────────────────────────

    @GetMapping("/usuarios/pendientes")
    public ResponseEntity<?> usuariosPendientes() {
        var pendientes = usuarioRepo.findAll().stream()
                .filter(u -> "no".equals(u.getAprobado()))
                .map(u -> Map.of(
                        "id", u.getIdentificador(),
                        "email", u.getEmail(),
                        "rol", u.getRol(),
                        "nombre", u.getPersona() != null ? u.getPersona().getNombre() : "",
                        "documento", u.getPersona() != null ? u.getPersona().getDocumento() : "",
                        "fechaRegistro", u.getFechaRegistro().toString()
                ))
                .toList();
        return ResponseEntity.ok(pendientes);
    }

    @PutMapping("/usuarios/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Integer id) {
        return usuarioRepo.findById(id).map(u -> {
            u.setAprobado("si");
            usuarioRepo.save(u);
            clienteRepo.findById(id).ifPresent(c -> {
                c.setAdmitido("si");
                clienteRepo.save(c);
            });

            String nombre = u.getPersona() != null ? u.getPersona().getNombre() : "usuario";
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(u.getEmail());
                msg.setSubject("¡Tu cuenta fue aprobada! — Subastas");
                msg.setText(
                    "Hola " + nombre + ",\n\n" +
                    "Tu solicitud de registro fue aprobada. Ya podés ingresar a la app con tu email y contraseña.\n\n" +
                    "Recordá que para poder pujar en subastas necesitás registrar al menos un medio de pago.\n\n" +
                    "¡Bienvenido/a!\n" +
                    "Equipo Subastas"
                );
                mailSender.send(msg);
            } catch (Exception ignored) {}

            return ResponseEntity.ok(Map.of("mensaje", "Usuario aprobado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    record RechazarRequest(String motivo) {}

    @PutMapping("/usuarios/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable Integer id, @RequestBody(required = false) RechazarRequest req) {
        return usuarioRepo.findById(id).map(u -> {
            u.setAprobado("no");
            usuarioRepo.save(u);
            clienteRepo.findById(id).ifPresent(c -> {
                c.setAdmitido("no");
                clienteRepo.save(c);
            });

            String motivo = (req != null && req.motivo() != null && !req.motivo().isBlank())
                    ? req.motivo()
                    : "Tu solicitud de registro fue rechazada.";

            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(u.getEmail());
                msg.setSubject("Solicitud de registro rechazada — Subastas");
                msg.setText(
                    "Hola,\n\n" +
                    "Tu solicitud de registro fue revisada por un administrador y no fue aprobada.\n\n" +
                    "Motivo: " + motivo + "\n\n" +
                    "Si tenés alguna consulta, contactate con nosotros.\n\n" +
                    "Equipo Subastas"
                );
                mailSender.send(msg);
            } catch (Exception ignored) {}

            return ResponseEntity.ok(Map.of("mensaje", "Usuario rechazado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── SUBASTAS ───────────────────────────────────────────────────────────────

    record SubastaRequest(
        @NotNull LocalDate fecha,
        @NotNull LocalTime hora,
        @NotBlank String categoria,
        String ubicacion,
        Integer capacidadAsistentes,
        String tieneDeposito,
        String seguridadPropia
    ) {}

    @GetMapping("/subastas")
    public ResponseEntity<?> listarSubastas() {
        return ResponseEntity.ok(subastaRepo.findAll().stream().map(this::toMap).toList());
    }

    @PostMapping("/subastas")
    public ResponseEntity<?> crearSubasta(@Valid @RequestBody SubastaRequest req) {
        Subasta s = new Subasta();
        s.setFecha(req.fecha());
        s.setHora(req.hora());
        s.setEstado("abierta");
        s.setCategoria(req.categoria());
        s.setUbicacion(req.ubicacion());
        s.setCapacidadAsistentes(req.capacidadAsistentes());
        s.setTieneDeposito(req.tieneDeposito());
        s.setSeguridadPropia(req.seguridadPropia());
        s = subastaRepo.save(s);
        return ResponseEntity.status(201).body(toMap(s));
    }

    @PutMapping("/subastas/{id}/cerrar")
    public ResponseEntity<?> cerrarSubasta(@PathVariable Integer id) {
        return subastaRepo.findById(id).map(s -> {
            s.setEstado("cerrada");
            subastaRepo.save(s);
            return ResponseEntity.ok(Map.of("mensaje", "Subasta cerrada"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── STATS ADMIN ────────────────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        long pendientes = usuarioRepo.findAll().stream()
                .filter(u -> "no".equals(u.getAprobado())).count();
        long subastaActivas = subastaRepo.findByEstado("abierta").size();
        long totalUsuarios = usuarioRepo.count();

        return ResponseEntity.ok(Map.of(
                "usuariosPendientes", pendientes,
                "subastaActivas", subastaActivas,
                "totalUsuarios", totalUsuarios
        ));
    }

    private Map<String, Object> toMap(Subasta s) {
        return Map.of(
                "id", s.getIdentificador(),
                "fecha", s.getFecha() != null ? s.getFecha().toString() : "",
                "hora", s.getHora() != null ? s.getHora().toString() : "",
                "estado", s.getEstado() != null ? s.getEstado() : "",
                "categoria", s.getCategoria() != null ? s.getCategoria() : "",
                "ubicacion", s.getUbicacion() != null ? s.getUbicacion() : ""
        );
    }
}
