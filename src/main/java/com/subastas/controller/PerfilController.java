package com.subastas.controller;

import com.subastas.repository.*;
import com.subastas.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
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
    private final SeguroRepository seguroRepo;
    private final ProductoUbicacionRepository ubicacionRepo;
    private final DepositoRepository depositoRepo;
    private final DevolucionRepository devolucionRepo;
    private final DuenioCuentaRepository duenioCuentaRepo;
    private final FacturaRepository facturaRepo;

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

    // ── SEGUROS (Item 14) ──────────────────────────────────────────────────────

    @GetMapping("/seguros")
    public ResponseEntity<?> seguros(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var productos = solicitudRepo.findByClienteOrderByFechaSolicitudDesc(userId);
        var result = new java.util.ArrayList<Map<String, Object>>();

        for (var p : productos) {
            if (p.getSeguro() == null || p.getSeguro().isBlank()) continue;
            seguroRepo.findById(p.getSeguro()).ifPresent(s -> {
                Map<String, Object> m = new HashMap<>();
                m.put("producto", p.getIdentificador());
                m.put("titulo", p.getTitulo() != null ? p.getTitulo() : "Sin título");
                m.put("nropoliza", s.getNropoliza());
                m.put("compania", s.getCompania());
                m.put("importe", s.getImporte());
                m.put("polizaCombinada", s.getPolizaCombinada());
                result.add(m);
            });
        }
        return ResponseEntity.ok(result);
    }

    // ── UBICACIÓN DE PRODUCTOS (Item 15) ────────────────────────────────────────

    @GetMapping("/ubicacion")
    public ResponseEntity<?> ubicacion(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var productos = solicitudRepo.findByClienteOrderByFechaSolicitudDesc(userId);
        var result = new java.util.ArrayList<Map<String, Object>>();

        for (var p : productos) {
            var ub = ubicacionRepo.findByProducto(p.getIdentificador()).orElse(null);
            if (ub == null) continue;
            var dep = depositoRepo.findById(ub.getDeposito()).orElse(null);
            Map<String, Object> m = new HashMap<>();
            m.put("producto", p.getIdentificador());
            m.put("titulo", p.getTitulo() != null ? p.getTitulo() : "Sin título");
            m.put("deposito", dep != null ? dep.getNombre() : "Depósito #" + ub.getDeposito());
            m.put("direccion", dep != null ? dep.getDireccion() : "");
            m.put("ubicacionDetalle", ub.getUbicacionDetalle() != null ? ub.getUbicacionDetalle() : "");
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // ── CUENTA DE COBRO (Item 21) ──────────────────────────────────────────────

    @GetMapping("/cuenta")
    public ResponseEntity<?> cuenta(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var cuenta = duenioCuentaRepo.findByDuenio(userId).orElse(null);
        if (cuenta == null) return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(Map.of(
                "banco", cuenta.getBanco() != null ? cuenta.getBanco() : "",
                "tipoCuenta", cuenta.getTipoCuenta() != null ? cuenta.getTipoCuenta() : "",
                "numeroCuenta", cuenta.getNumeroCuenta() != null ? cuenta.getNumeroCuenta() : "",
                "moneda", cuenta.getMoneda(),
                "esExterior", cuenta.getEsExterior()
        ));
    }

    record CuentaRequest(String banco, String tipoCuenta, String numeroCuenta, String moneda, String esExterior) {}

    @PutMapping("/cuenta")
    public ResponseEntity<?> actualizarCuenta(@RequestBody CuentaRequest req, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var cuenta = duenioCuentaRepo.findByDuenio(userId).orElseGet(() -> {
            var nueva = new DuenioCuenta();
            nueva.setDuenio(userId);
            return nueva;
        });
        if (req.banco() != null) cuenta.setBanco(req.banco());
        if (req.tipoCuenta() != null) cuenta.setTipoCuenta(req.tipoCuenta());
        if (req.numeroCuenta() != null) cuenta.setNumeroCuenta(req.numeroCuenta());
        if (req.moneda() != null) cuenta.setMoneda(req.moneda());
        if (req.esExterior() != null) cuenta.setEsExterior(req.esExterior());
        duenioCuentaRepo.save(cuenta);

        return ResponseEntity.ok(Map.of("mensaje", "Cuenta de cobro actualizada"));
    }

    // ── FACTURAS (Items 17-19) ─────────────────────────────────────────────────

    @GetMapping("/facturas")
    public ResponseEntity<?> facturas(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var result = facturaRepo.findByCliente(userId).stream()
                .map(f -> {
                    String titulo = "Artículo";
                    if (f.getRegistro() != null) {
                        var reg = registroRepo.findById(f.getRegistro()).orElse(null);
                        if (reg != null && reg.getProducto() != null) {
                            var prod = solicitudRepo.findById(reg.getProducto()).orElse(null);
                            if (prod != null && prod.getTitulo() != null) titulo = prod.getTitulo();
                        }
                    }
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", f.getIdentificador());
                    m.put("titulo", titulo);
                    m.put("importePujado", f.getImportePujado());
                    m.put("comision", f.getComision());
                    m.put("costoEnvio", f.getCostoEnvio());
                    m.put("total", f.getTotal());
                    m.put("tipoEntrega", f.getTipoEntrega() != null ? f.getTipoEntrega() : "");
                    m.put("conSeguro", f.getConSeguro());
                    m.put("emitida", f.getEmitida() != null ? f.getEmitida().toString() : "");
                    return m;
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    // Item 18: Actualizar tipo de entrega y dirección de envío en factura
    record EntregaRequest(String tipoEntrega, String direccionEnvio, String conSeguro) {}

    @PutMapping("/facturas/{id}/entrega")
    public ResponseEntity<?> actualizarEntrega(@PathVariable Integer id,
                                                @RequestBody EntregaRequest req,
                                                Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        return facturaRepo.findById(id).map(f -> {
            if (!f.getCliente().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));
            if (req.tipoEntrega() != null) {
                if (!List.of("envio", "retiro_personal").contains(req.tipoEntrega()))
                    return ResponseEntity.badRequest().body(Map.of("error", "tipoEntrega debe ser 'envio' o 'retiro_personal'"));
                f.setTipoEntrega(req.tipoEntrega());
            }
            if (req.direccionEnvio() != null) f.setDireccionEnvio(req.direccionEnvio());
            if (req.conSeguro() != null) {
                if (!List.of("si", "no").contains(req.conSeguro()))
                    return ResponseEntity.badRequest().body(Map.of("error", "conSeguro debe ser 'si' o 'no'"));
                f.setConSeguro(req.conSeguro());
            }
            facturaRepo.save(f);
            return ResponseEntity.ok(Map.of("mensaje", "Datos de entrega actualizados"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── DEVOLUCIONES (Item 20) ─────────────────────────────────────────────────

    @GetMapping("/devoluciones")
    public ResponseEntity<?> devoluciones(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var productos = solicitudRepo.findByClienteOrderByFechaSolicitudDesc(userId).stream()
                .map(p -> p.getIdentificador())
                .toList();

        var result = new java.util.ArrayList<Map<String, Object>>();
        for (var prodId : productos) {
            for (var d : devolucionRepo.findByProducto(prodId)) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", d.getIdentificador());
                m.put("producto", d.getProducto());
                m.put("motivo", d.getMotivo());
                m.put("cargo", d.getCargo());
                m.put("fecha", d.getFecha() != null ? d.getFecha().toString() : "");
                m.put("estado", d.getEstado());
                result.add(m);
            }
        }
        return ResponseEntity.ok(result);
    }

    record DevolucionRequest(Integer productoId, String motivo) {}

    @PostMapping("/devoluciones")
    public ResponseEntity<?> solicitarDevolucion(@RequestBody DevolucionRequest req, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        if (req.productoId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "productoId es obligatorio"));
        if (req.motivo() == null || req.motivo().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "El motivo es obligatorio"));

        Integer userId = usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).build();

        var prod = solicitudRepo.findById(req.productoId()).orElse(null);
        if (prod == null) return ResponseEntity.notFound().build();
        if (!prod.getCliente().equals(userId))
            return ResponseEntity.status(403).body(Map.of("error", "No autorizado"));

        Devolucion d = new Devolucion();
        d.setProducto(req.productoId());
        d.setMotivo(req.motivo());
        d.setEstado("pendiente");
        devolucionRepo.save(d);

        return ResponseEntity.ok(Map.of("mensaje", "Devolución registrada. Un administrador la revisará."));
    }
}
