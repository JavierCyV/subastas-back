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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
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
    private final SolicitudItemRepository solicitudRepo;
    private final SubastaMonedaRepository subastaMonedaRepo;
    private final MetodoPagoRepository metodoPagoRepo;
    private final MetodoPagoVerificacionRepository metodoPagoVerificacionRepo;
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

    record AprobarRequest(String categoria) {}

    @PutMapping("/usuarios/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable Integer id, @RequestBody(required = false) AprobarRequest req) {
        return usuarioRepo.findById(id).map(u -> {
            u.setAprobado("si");
            usuarioRepo.save(u);
            clienteRepo.findById(id).ifPresent(c -> {
                c.setAdmitido("si");
                if (req != null && req.categoria() != null) c.setCategoria(req.categoria());
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
        String seguridadPropia,
        String moneda
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

        if (req.moneda() != null && !req.moneda().isBlank()) {
            SubastaMoneda sm = new SubastaMoneda();
            sm.setSubastaId(s.getIdentificador());
            sm.setMoneda(req.moneda().toUpperCase());
            subastaMonedaRepo.save(sm);
        }

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

    // ── SOLICITUDES DE ITEMS ───────────────────────────────────────────────────

    @GetMapping("/solicitudes")
    public ResponseEntity<?> listarSolicitudes() {
        var lista = solicitudRepo.findAll().stream()
                .map(this::solicitudToMap)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/solicitudes/{id}/inspeccionar")
    public ResponseEntity<?> marcarInspeccion(@PathVariable Integer id) {
        return solicitudRepo.findById(id).map(s -> {
            s.setEstado("inspeccion");
            solicitudRepo.save(s);
            return ResponseEntity.ok(Map.of("mensaje", "Solicitud marcada en inspección"));
        }).orElse(ResponseEntity.notFound().build());
    }

    record TasarRequest(BigDecimal precioBaseOficial, BigDecimal comision) {}

    @PutMapping("/solicitudes/{id}/tasar")
    public ResponseEntity<?> tasar(@PathVariable Integer id, @RequestBody TasarRequest req) {
        return solicitudRepo.findById(id).map(s -> {
            if (req.precioBaseOficial() == null || req.comision() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Precio base y comisión son obligatorios"));
            s.setEstado("tasado");
            s.setPrecioBaseOficial(req.precioBaseOficial());
            s.setComision(req.comision());
            solicitudRepo.save(s);
            return ResponseEntity.ok(solicitudToMap(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    record RechazarSolicitudRequest(String motivo) {}

    @PutMapping("/solicitudes/{id}/rechazar")
    public ResponseEntity<?> rechazarSolicitud(@PathVariable Integer id,
                                                @RequestBody(required = false) RechazarSolicitudRequest req) {
        return solicitudRepo.findById(id).map(s -> {
            s.setEstado("rechazado_empresa");
            s.setMotivoRechazo(req != null ? req.motivo() : "Rechazado por la empresa");
            solicitudRepo.save(s);
            return ResponseEntity.ok(Map.of("mensaje", "Solicitud rechazada"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── VERIFICAR MEDIO DE PAGO ─────────────────────────────────────────────

    @PutMapping("/pagos/{id}/verificar")
    public ResponseEntity<?> verificarPago(@PathVariable Integer id) {
        return metodoPagoRepo.findById(id).map(p -> {
            var v = metodoPagoVerificacionRepo.findById(id).orElseGet(() -> {
                var nuevo = new com.subastas.entity.MetodoPagoVerificacion();
                nuevo.setMetodoPagoId(id);
                return nuevo;
            });
            v.setVerificado("si");
            metodoPagoVerificacionRepo.save(v);
            return ResponseEntity.ok(Map.of("mensaje", "Método de pago verificado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> solicitudToMap(com.subastas.entity.SolicitudItem s) {
        var map = new HashMap<String, Object>();
        map.put("id", s.getIdentificador());
        map.put("cliente", s.getCliente());
        map.put("titulo", s.getTitulo());
        map.put("categoria", s.getCategoria() != null ? s.getCategoria() : "");
        map.put("descripcion", s.getDescripcion() != null ? s.getDescripcion() : "");
        map.put("estado", s.getEstado());
        map.put("precioBaseSugerido", s.getPrecioBaseSugerido());
        map.put("precioBaseOficial", s.getPrecioBaseOficial());
        map.put("comision", s.getComision());
        map.put("motivoRechazo", s.getMotivoRechazo() != null ? s.getMotivoRechazo() : "");
        map.put("fechaSolicitud", s.getFechaSolicitud() != null ? s.getFechaSolicitud().toString() : "");
        return map;
    }

    private Map<String, Object> toMap(Subasta s) {
        String moneda = subastaMonedaRepo.findBySubastaId(s.getIdentificador())
                .map(sm -> sm.getMoneda())
                .orElse("ARS");
        return Map.of(
                "id", s.getIdentificador(),
                "fecha", s.getFecha() != null ? s.getFecha().toString() : "",
                "hora", s.getHora() != null ? s.getHora().toString() : "",
                "estado", s.getEstado() != null ? s.getEstado() : "",
                "categoria", s.getCategoria() != null ? s.getCategoria() : "",
                "ubicacion", s.getUbicacion() != null ? s.getUbicacion() : "",
                "moneda", moneda
        );
    }
}
