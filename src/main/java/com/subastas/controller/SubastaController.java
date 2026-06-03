package com.subastas.controller;

import com.subastas.entity.Asistente;
import com.subastas.entity.Pujo;
import com.subastas.repository.AsistenteRepository;
import com.subastas.repository.CatalogoRepository;
import com.subastas.repository.ClienteRepository;
import com.subastas.repository.ItemCatalogoRepository;
import com.subastas.repository.MetodoPagoRepository;
import com.subastas.repository.PujoRepository;
import com.subastas.repository.RegistroDeSubastaRepository;
import com.subastas.repository.SubastaRepository;
import com.subastas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subastas")
@RequiredArgsConstructor
public class SubastaController {

    private final SubastaRepository subastaRepo;
    private final CatalogoRepository catalogoRepo;
    private final ItemCatalogoRepository itemCatalogoRepo;
    private final PujoRepository pujoRepo;
    private final AsistenteRepository asistenteRepo;
    private final RegistroDeSubastaRepository registroRepo;
    private final UsuarioRepository usuarioRepo;
    private final ClienteRepository clienteRepo;
    private final MetodoPagoRepository metodoPagoRepo;

    // Orden de categorías para control de acceso
    private static final List<String> ORDEN_CATEGORIAS = List.of("comun", "especial", "plata", "oro", "platino");

    private boolean categoriaPermite(String catUsuario, String catSubasta) {
        int iUsuario  = ORDEN_CATEGORIAS.indexOf(catUsuario  != null ? catUsuario.toLowerCase()  : "comun");
        int iSubasta  = ORDEN_CATEGORIAS.indexOf(catSubasta  != null ? catSubasta.toLowerCase()  : "comun");
        return iUsuario >= iSubasta;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) String categoria) {
        var hoy = java.time.LocalDate.now();
        var estados = List.of("abierta", "cerrada");
        var categoriaNormalizada = categoria != null ? categoria.trim() : null;
        var subastas = (categoriaNormalizada == null || categoriaNormalizada.isBlank() || "todas".equalsIgnoreCase(categoriaNormalizada))
                ? subastaRepo.findByEstadoIn(estados)
                : subastaRepo.findByEstadoInAndCategoriaIgnoreCase(estados, categoriaNormalizada);

        var result = subastas.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id",        s.getIdentificador());
            map.put("categoria", s.getCategoria());
            map.put("estado",    s.getEstado());
            map.put("fecha",     s.getFecha() != null ? s.getFecha().toString() : "");
            map.put("hora",      s.getHora()  != null ? s.getHora().toString()  : "");
            map.put("ubicacion", s.getUbicacion() != null ? s.getUbicacion() : "");

            // tipo: en_vivo | proxima | cerrada
            String tipo;
            if ("cerrada".equals(s.getEstado())) {
                tipo = "cerrada";
            } else if (s.getFecha() != null && !s.getFecha().isAfter(hoy)) {
                tipo = "en_vivo";
            } else {
                tipo = "proxima";
            }
            map.put("tipo", tipo);

            // Catalogo info
            catalogoRepo.findBySubasta(s.getIdentificador()).ifPresent(cat -> {
                map.put("descripcion", cat.getDescripcion());
                var items = itemCatalogoRepo.findByCatalogo(cat.getIdentificador());
                map.put("totalItems", items.size());
                items.stream()
                    .map(i -> i.getPreciobase())
                    .min(BigDecimal::compareTo)
                    .ifPresent(min -> map.put("precioBaseMinimo", min));
            });

            if (!map.containsKey("descripcion"))    map.put("descripcion",    "");
            if (!map.containsKey("totalItems"))      map.put("totalItems",      0);
            if (!map.containsKey("precioBaseMinimo")) map.put("precioBaseMinimo", BigDecimal.ZERO);

            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/catalogo")
    public ResponseEntity<?> catalogo(@PathVariable Integer id) {
        var cat = catalogoRepo.findBySubasta(id).orElse(null);
        if (cat == null) return ResponseEntity.ok(List.of());

        var items = itemCatalogoRepo.findByCatalogo(cat.getIdentificador()).stream()
            .map(i -> Map.<String, Object>of(
                "id",         i.getIdentificador(),
                "producto",   i.getProducto(),
                "precioBase", i.getPreciobase(),
                "comision",   i.getComision(),
                "subastado",  i.getSubastado() != null ? i.getSubastado() : "no"
            ))
            .toList();

        return ResponseEntity.ok(Map.of(
            "catalogoId",   cat.getIdentificador(),
            "descripcion",  cat.getDescripcion(),
            "items",        items
        ));
    }

    @GetMapping("/{id}/resultado")
    public ResponseEntity<?> resultado(@PathVariable Integer id, Authentication auth) {
        var subasta = subastaRepo.findById(id).orElse(null);
        if (subasta == null) return ResponseEntity.status(404).build();

        // ID del usuario autenticado
        Integer userId = auth != null
                ? usuarioRepo.findByEmail(auth.getName()).map(u -> u.getIdentificador()).orElse(null)
                : null;

        // Descripción del catálogo
        String descripcion = catalogoRepo.findBySubasta(id)
                .map(c -> c.getDescripcion())
                .orElse("Subasta #" + id);

        // Items del catálogo
        var items = catalogoRepo.findBySubasta(id)
                .map(c -> itemCatalogoRepo.findByCatalogo(c.getIdentificador()))
                .orElse(List.of());
        Set<Integer> itemIds = items.stream()
                .map(i -> i.getIdentificador())
                .collect(Collectors.toSet());

        // Asistente del usuario en esta subasta (para identificar sus pujas)
        Integer miAsistenteId = userId != null
                ? asistenteRepo.findByCliente(userId).stream()
                        .filter(a -> a.getSubasta().equals(id))
                        .findFirst()
                        .map(a -> a.getIdentificador())
                        .orElse(null)
                : null;

        // Número de postor para mostrar
        Integer miNumeroPostor = userId != null
                ? asistenteRepo.findByCliente(userId).stream()
                        .filter(a -> a.getSubasta().equals(id))
                        .findFirst()
                        .map(a -> a.getNumeropostor())
                        .orElse(null)
                : null;

        // Todas las pujas de la subasta, ordenadas por importe desc
        var pujos = pujoRepo.findBySubastaId(id).stream()
                .filter(p -> itemIds.contains(p.getItem()))
                .map(p -> {
                    // Obtener el número de postor del asistente
                    var asistente = asistenteRepo.findById(p.getAsistente()).orElse(null);
                    int numPostor = asistente != null ? asistente.getNumeropostor() : 0;
                    boolean esYo  = p.getAsistente().equals(miAsistenteId);
                    return Map.<String, Object>of(
                            "numeropostor", esYo ? "Vos"   : "Postor #" + numPostor,
                            "importe",      p.getImporte(),
                            "esYo",         esYo,
                            "ganador",      "si".equals(p.getGanador())
                    );
                })
                .collect(Collectors.toList());

        // Estado para el usuario actual
        boolean gano = userId != null && registroRepo.findByCliente(userId).stream()
                .anyMatch(r -> r.getSubasta().equals(id));
        boolean participo = userId != null && miAsistenteId != null;
        String estado = gano ? "GANADA" : (participo ? "PARTICIPÓ" : "CERRADA");

        // Mi importe ganador (si ganó)
        var miImporte = registroRepo.findByCliente(userId != null ? userId : -1).stream()
                .filter(r -> r.getSubasta().equals(id))
                .findFirst()
                .map(r -> r.getImporte())
                .orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("subastaId",   id);
        result.put("descripcion", descripcion);
        result.put("fecha",       subasta.getFecha() != null ? subasta.getFecha().toString() : "");
        result.put("categoria",   subasta.getCategoria() != null ? subasta.getCategoria() : "");
        result.put("estado",      estado);
        result.put("miImporte",   miImporte);
        result.put("pujos",       pujos);

        return ResponseEntity.ok(result);
    }

    // ── UNIRSE A SUBASTA ───────────────────────────────────────────────────────

    @PostMapping("/{id}/unirse")
    public ResponseEntity<?> unirse(@PathVariable Integer id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        var subasta = subastaRepo.findById(id).orElse(null);
        if (subasta == null) return ResponseEntity.status(404).body(Map.of("error", "Subasta no encontrada"));
        if (!"abierta".equals(subasta.getEstado()))
            return ResponseEntity.badRequest().body(Map.of("error", "La subasta no está abierta"));

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));

        // Verificar categoría del usuario
        var cliente = clienteRepo.findById(userId).orElse(null);
        String catUsuario = cliente != null ? cliente.getCategoria() : "comun";
        if (!categoriaPermite(catUsuario, subasta.getCategoria()))
            return ResponseEntity.status(403).body(Map.of("error", "Categoría insuficiente para esta subasta"));

        // Verificar si ya es asistente en ESTA subasta
        var existente = asistenteRepo.findByCliente(userId).stream()
                .filter(a -> a.getSubasta().equals(id)).findFirst();
        if (existente.isPresent()) {
            var a = existente.get();
            boolean tienePago = !metodoPagoRepo.findByClienteAndActivo(userId, "si").isEmpty();
            return ResponseEntity.ok(Map.of(
                    "asistenteId", a.getIdentificador(),
                    "numeropostor", a.getNumeropostor(),
                    "puedePublicar", tienePago
            ));
        }

        // Un usuario no puede estar en más de una subasta abierta a la vez
        boolean yaEnOtra = asistenteRepo.findByCliente(userId).stream()
                .anyMatch(a -> !a.getSubasta().equals(id) &&
                        subastaRepo.findById(a.getSubasta())
                                .map(s -> "abierta".equals(s.getEstado()))
                                .orElse(false));
        if (yaEnOtra)
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Ya estás participando en otra subasta abierta. Salí de esa subasta antes de unirte a una nueva."));

        // Crear asistente con número de postor siguiente
        int siguiente = (int) asistenteRepo.findAll().stream()
                .filter(a -> a.getSubasta().equals(id)).count() + 1;

        Asistente a = new Asistente();
        a.setCliente(userId);
        a.setSubasta(id);
        a.setNumeropostor(siguiente);
        asistenteRepo.save(a);

        boolean tienePago = !metodoPagoRepo.findByClienteAndActivo(userId, "si").isEmpty();
        return ResponseEntity.status(201).body(Map.of(
                "asistenteId", a.getIdentificador(),
                "numeropostor", siguiente,
                "puedePublicar", tienePago
        ));
    }

    // ── ESTADO ACTUAL DE SUBASTA (para polling) ────────────────────────────────

    @GetMapping("/{id}/estado")
    public ResponseEntity<?> estadoActual(@PathVariable Integer id) {
        var subasta = subastaRepo.findById(id).orElse(null);
        if (subasta == null) return ResponseEntity.status(404).build();

        var cat = catalogoRepo.findBySubasta(id).orElse(null);
        if (cat == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("subastaId",  id);
            empty.put("estado",     subasta.getEstado());
            empty.put("totalItems", 0);
            empty.put("itemActual", null);
            return ResponseEntity.ok(empty);
        }

        var todosItems = itemCatalogoRepo.findByCatalogo(cat.getIdentificador());
        int total = todosItems.size();

        // Item actual = primer item no subastado (en orden de identificador)
        var itemActual = todosItems.stream()
                .filter(i -> !"si".equals(i.getSubastado()))
                .findFirst()
                .orElse(null);

        String descCatalogo = cat.getDescripcion();

        Map<String, Object> itemActualMap = null;
        if (itemActual != null) {
            int numero = todosItems.indexOf(itemActual) + 1;

            // Mejor oferta para el item actual
            BigDecimal mejorImporte = pujoRepo.findAll().stream()
                    .filter(p -> p.getItem().equals(itemActual.getIdentificador()) && "si".equals(p.getGanador()))
                    .map(Pujo::getImporte)
                    .findFirst()
                    .orElse(null);

            // Pujas del item actual, ordenadas por importe desc
            var pujos = pujoRepo.findAll().stream()
                    .filter(p -> p.getItem().equals(itemActual.getIdentificador()))
                    .sorted((a, b) -> b.getImporte().compareTo(a.getImporte()))
                    .limit(10)
                    .map(p -> {
                        var asistente = asistenteRepo.findById(p.getAsistente()).orElse(null);
                        int numPostor = asistente != null ? asistente.getNumeropostor() : 0;
                        return Map.<String, Object>of(
                                "numPostor", numPostor,
                                "importe",   p.getImporte(),
                                "ganador",   "si".equals(p.getGanador())
                        );
                    })
                    .toList();

            itemActualMap = new HashMap<>();
            itemActualMap.put("itemId",      itemActual.getIdentificador());
            itemActualMap.put("numero",      numero);
            itemActualMap.put("descripcion", descCatalogo);
            itemActualMap.put("precioBase",  itemActual.getPreciobase());
            itemActualMap.put("mejorOferta", mejorImporte);
            itemActualMap.put("pujos",       pujos);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("subastaId",  id);
        result.put("estado",     subasta.getEstado());
        result.put("totalItems", total);
        result.put("itemActual", itemActualMap);
        return ResponseEntity.ok(result);
    }

    // ── PUJAR ─────────────────────────────────────────────────────────────────

    record PujarRequest(BigDecimal importe) {}

    @PostMapping("/{id}/items/{itemId}/pujar")
    public ResponseEntity<?> pujar(@PathVariable Integer id,
                                   @PathVariable Integer itemId,
                                   @RequestBody PujarRequest req,
                                   Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        if (req.importe() == null || req.importe().compareTo(BigDecimal.ZERO) <= 0)
            return ResponseEntity.badRequest().body(Map.of("error", "Importe inválido"));

        var subasta = subastaRepo.findById(id).orElse(null);
        if (subasta == null) return ResponseEntity.status(404).body(Map.of("error", "Subasta no encontrada"));
        if (!"abierta".equals(subasta.getEstado()))
            return ResponseEntity.badRequest().body(Map.of("error", "La subasta no está abierta"));

        var item = itemCatalogoRepo.findById(itemId).orElse(null);
        if (item == null) return ResponseEntity.status(404).body(Map.of("error", "Item no encontrado"));
        if ("si".equals(item.getSubastado()))
            return ResponseEntity.badRequest().body(Map.of("error", "Este item ya fue subastado"));

        Integer userId = usuarioRepo.findByEmail(auth.getName())
                .map(u -> u.getIdentificador()).orElse(null);
        if (userId == null) return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));

        // Verificar que el usuario es asistente en esta subasta
        var asistente = asistenteRepo.findByCliente(userId).stream()
                .filter(a -> a.getSubasta().equals(id)).findFirst().orElse(null);
        if (asistente == null)
            return ResponseEntity.badRequest().body(Map.of("error", "No está unido a esta subasta. Use /unirse primero."));

        // Verificar medio de pago
        if (metodoPagoRepo.findByClienteAndActivo(userId, "si").isEmpty())
            return ResponseEntity.status(403).body(Map.of("error", "Necesita al menos un medio de pago activo para pujar"));

        // Mejor oferta actual para este item
        BigDecimal mejorActual = pujoRepo.findAll().stream()
                .filter(p -> p.getItem().equals(itemId) && "si".equals(p.getGanador()))
                .map(Pujo::getImporte)
                .findFirst()
                .orElse(item.getPreciobase());

        // Validar límites (no aplica a oro/platino)
        String catSubasta = subasta.getCategoria() != null ? subasta.getCategoria().toLowerCase() : "";
        boolean sinLimites = catSubasta.equals("oro") || catSubasta.equals("platino");

        if (!sinLimites) {
            BigDecimal minPuja = mejorActual.add(item.getPreciobase().multiply(new BigDecimal("0.01")));
            BigDecimal maxPuja = mejorActual.add(item.getPreciobase().multiply(new BigDecimal("0.20")));

            if (req.importe().compareTo(minPuja) < 0)
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "La puja mínima es " + minPuja,
                        "minPuja", minPuja,
                        "maxPuja", maxPuja
                ));
            if (req.importe().compareTo(maxPuja) > 0)
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "La puja máxima es " + maxPuja,
                        "minPuja", minPuja,
                        "maxPuja", maxPuja
                ));
        } else {
            // Subastas oro/platino: solo debe ser mayor a la mejor oferta
            if (req.importe().compareTo(mejorActual) <= 0)
                return ResponseEntity.badRequest().body(Map.of("error", "La puja debe ser mayor a la mejor oferta actual: " + mejorActual));
        }

        // Desmarcar el ganador anterior
        pujoRepo.findAll().stream()
                .filter(p -> p.getItem().equals(itemId) && "si".equals(p.getGanador()))
                .forEach(p -> { p.setGanador("no"); pujoRepo.save(p); });

        // Registrar nueva puja
        Pujo nuevoPujo = new Pujo();
        nuevoPujo.setAsistente(asistente.getIdentificador());
        nuevoPujo.setItem(itemId);
        nuevoPujo.setImporte(req.importe());
        nuevoPujo.setGanador("si");
        pujoRepo.save(nuevoPujo);

        return ResponseEntity.status(201).body(Map.of(
                "mensaje",     "Puja registrada",
                "importe",     nuevoPujo.getImporte(),
                "numeropostor", asistente.getNumeropostor()
        ));
    }
}
