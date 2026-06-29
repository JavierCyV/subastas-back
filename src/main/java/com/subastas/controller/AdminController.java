package com.subastas.controller;

import com.subastas.entity.*;
import com.subastas.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.subastas.service.ResendMailService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
    private final RegistroPendienteRepository registroPendienteRepo;
    private final ProductoOrigenRepository productoOrigenRepo;
    private final DevolucionRepository devolucionRepo;
    private final ResendMailService mailService;
    private static final SecureRandom RNG = new SecureRandom();

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
                if (req != null && req.categoria() != null && !req.categoria().trim().isEmpty()) {
                    c.setCategoria(req.categoria());
                } else if (c.getCategoria() == null || c.getCategoria().trim().isEmpty()) {
                    c.setCategoria("comun");
                }
                clienteRepo.save(c);
            });

            String nombre = u.getPersona() != null ? u.getPersona().getNombre() : "usuario";
            try {
                mailService.send(u.getEmail(), "¡Tu cuenta fue aprobada! — Subastas",
                    "Hola " + nombre + ",\n\n" +
                    "Tu solicitud de registro fue aprobada. Ya podés ingresar a la app con tu email y contraseña.\n\n" +
                    "Recordá que para poder pujar en subastas necesitás registrar al menos un medio de pago.\n\n" +
                    "¡Bienvenido/a!\n" +
                    "Equipo Subastas");
            } catch (Exception e) { log.warn("No se pudo enviar email de aprobación a {}: {}", u.getEmail(), e.getMessage()); }

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
                mailService.send(u.getEmail(), "Solicitud de registro rechazada — Subastas",
                    "Hola,\n\n" +
                    "Tu solicitud de registro fue revisada por un administrador y no fue aprobada.\n\n" +
                    "Motivo: " + motivo + "\n\n" +
                    "Si tenés alguna consulta, contactate con nosotros.\n\n" +
                    "Equipo Subastas");
            } catch (Exception e) { log.warn("No se pudo enviar email de rechazo a {}: {}", u.getEmail(), e.getMessage()); }

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

    // ── REGISTROS PENDIENTES (Item 12: 2 etapas) ─────────────────────────────

    @GetMapping("/registros/pendientes")
    public ResponseEntity<?> registrosPendientes() {
        var lista = registroPendienteRepo.findByCodigoCompletarIsNull().stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getIdentificador(),
                        "email", r.getEmail(),
                        "nombre", r.getNombre(),
                        "documento", r.getDocumento(),
                        "rol", r.getRol(),
                        "fotoDniFrente", r.getFotoDniFrente() != null ? r.getFotoDniFrente() : "",
                        "fotoDniDorso", r.getFotoDniDorso() != null ? r.getFotoDniDorso() : "",
                        "creado", r.getCreado() != null ? r.getCreado().toString() : ""
                ))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/registros/{id}/enviar-codigo")
    public ResponseEntity<?> enviarCodigo(@PathVariable Integer id) {
        return registroPendienteRepo.findById(id).map(r -> {
            String codigo = String.format("%06d", RNG.nextInt(1000000));
            r.setCodigoCompletar(codigo);
            r.setCodigoExpiracion(LocalDateTime.now().plusHours(48));
            registroPendienteRepo.save(r);

            try {
                mailService.send(r.getEmail(), "Completá tu registro — Subastas",
                    "Hola " + r.getNombre() + ",\n\n" +
                    "Tu preregistro fue aprobado. Usá el siguiente código para completar tu registro:\n\n" +
                    "Código: " + codigo + "\n\n" +
                    "El código expira en 48 horas.\n\n" +
                    "Ingresá a la app y usá la opción 'Completar registro' con tu email y este código.\n\n" +
                    "Equipo Subastas");
            } catch (Exception e) { log.warn("No se pudo enviar código de preregistro a {}: {}", r.getEmail(), e.getMessage()); }

            return ResponseEntity.ok(Map.of("mensaje", "Código enviado al email del usuario"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/registros/{id}")
    public ResponseEntity<?> rechazarPreRegistro(@PathVariable Integer id) {
        return registroPendienteRepo.findById(id).map(r -> {
            registroPendienteRepo.delete(r);
            return ResponseEntity.ok(Map.of("mensaje", "Preregistro rechazado y eliminado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── VERIFICAR DOCUMENTOS DE ORIGEN (Item 22) ─────────────────────────────

    @GetMapping("/origen")
    public ResponseEntity<?> listarOrigenPendiente() {
        var lista = productoOrigenRepo.findAll().stream()
                .filter(o -> !"si".equals(o.getVerificado()))
                .map(o -> {
                    var prod = solicitudRepo.findById(o.getProducto()).orElse(null);
                    return Map.<String, Object>of(
                            "producto", o.getProducto(),
                            "titulo", prod != null && prod.getTitulo() != null ? prod.getTitulo() : "",
                            "tipoDocumento", o.getTipoDocumento() != null ? o.getTipoDocumento() : "",
                            "archivo", o.getArchivo() != null ? o.getArchivo() : "",
                            "verificado", o.getVerificado()
                    );
                })
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PutMapping("/origen/{productoId}/verificar")
    public ResponseEntity<?> verificarOrigen(@PathVariable Integer productoId) {
        return productoOrigenRepo.findByProducto(productoId).map(o -> {
            o.setVerificado("si");
            productoOrigenRepo.save(o);
            return ResponseEntity.ok(Map.of("mensaje", "Documento de origen verificado"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DEVOLUCIONES (Item 20) ─────────────────────────────────────────────────

    @GetMapping("/devoluciones")
    public ResponseEntity<?> listarDevoluciones() {
        var lista = devolucionRepo.findAll().stream()
                .map(d -> {
                    var prod = solicitudRepo.findById(d.getProducto()).orElse(null);
                    return Map.<String, Object>of(
                            "id", d.getIdentificador(),
                            "producto", d.getProducto(),
                            "titulo", prod != null && prod.getTitulo() != null ? prod.getTitulo() : "",
                            "motivo", d.getMotivo(),
                            "cargo", d.getCargo(),
                            "fecha", d.getFecha() != null ? d.getFecha().toString() : "",
                            "estado", d.getEstado()
                    );
                })
                .toList();
        return ResponseEntity.ok(lista);
    }

    record AprobarDevolucionRequest(BigDecimal cargo) {}

    @PutMapping("/devoluciones/{id}/aprobar")
    public ResponseEntity<?> aprobarDevolucion(@PathVariable Integer id,
                                                @RequestBody(required = false) AprobarDevolucionRequest req) {
        return devolucionRepo.findById(id).map(d -> {
            d.setEstado("aprobada");
            if (req != null && req.cargo() != null) d.setCargo(req.cargo());
            devolucionRepo.save(d);
            return ResponseEntity.ok(Map.of("mensaje", "Devolución aprobada"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/devoluciones/{id}/rechazar")
    public ResponseEntity<?> rechazarDevolucion(@PathVariable Integer id) {
        return devolucionRepo.findById(id).map(d -> {
            d.setEstado("rechazada");
            devolucionRepo.save(d);
            return ResponseEntity.ok(Map.of("mensaje", "Devolución rechazada"));
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
