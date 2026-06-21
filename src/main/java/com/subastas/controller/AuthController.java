package com.subastas.controller;

import com.subastas.entity.Cliente;
import com.subastas.entity.Duenio;
import com.subastas.entity.Persona;
import com.subastas.entity.Usuario;
import com.subastas.repository.ClienteRepository;
import com.subastas.repository.DuenioRepository;
import com.subastas.repository.PersonaRepository;
import com.subastas.repository.UsuarioRepository;
import com.subastas.security.JwtService;
import com.subastas.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepo;
    private final PersonaRepository personaRepo;
    private final ClienteRepository clienteRepo;
    private final DuenioRepository duenioRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final PasswordResetService resetService;

    // ── LOGIN ──────────────────────────────────────────────────────────────────
    record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var usuario = usuarioRepo.findByEmail(req.email())
                .orElse(null);

        if (usuario == null || !encoder.matches(req.password(), usuario.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales inválidas"));
        }

        if (!"si".equals(usuario.getAprobado())) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Tu cuenta está pendiente de aprobación por un administrador"));
        }

        String token = jwtService.generateToken(
                usuario.getEmail(), usuario.getRol(), usuario.getIdentificador());

        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("token",  token);
        resp.put("rol",    usuario.getRol());
        resp.put("userId", usuario.getIdentificador());
        resp.put("nombre", usuario.getPersona() != null ? usuario.getPersona().getNombre() : "");

        if ("cliente".equals(usuario.getRol())) {
            clienteRepo.findById(usuario.getIdentificador())
                    .ifPresent(c -> resp.put("categoria", c.getCategoria()));
        }
        if (!resp.containsKey("categoria")) resp.put("categoria", null);

        return ResponseEntity.ok(resp);
    }

    // ── REGISTRO ───────────────────────────────────────────────────────────────
    record RegisterRequest(
        @NotBlank String nombre,
        @NotBlank String documento,
        String direccion,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String rol          // cliente | duenio
    ) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (usuarioRepo.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El email ya está registrado"));
        }

        // 1. Crear persona
        Persona persona = new Persona();
        persona.setDocumento(req.documento());
        persona.setNombre(req.nombre());
        persona.setDireccion(req.direccion());
        persona.setEstado("activo");
        persona = personaRepo.save(persona);

        // 2. Crear perfil de rol (sin verificar — queda pendiente de aprobación)
        if ("cliente".equals(req.rol())) {
            Cliente c = new Cliente();
            c.setIdentificador(persona.getIdentificador());
            c.setCategoria("comun");
            // verificador = 1 (admin del sistema); se puede configurar
            c.setVerificador(1);
            clienteRepo.save(c);
        } else if ("duenio".equals(req.rol())) {
            Duenio d = new Duenio();
            d.setIdentificador(persona.getIdentificador());
            d.setVerificador(1);
            duenioRepo.save(d);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol inválido. Usar: cliente o duenio"));
        }

        // 3. Crear usuario (aprobado = no → pendiente de admin)
        Usuario usuario = new Usuario();
        usuario.setIdentificador(persona.getIdentificador());
        usuario.setEmail(req.email());
        usuario.setPassword(encoder.encode(req.password()));
        usuario.setRol(req.rol());
        usuario.setAprobado("no");
        usuarioRepo.save(usuario);

        return ResponseEntity.status(201).body(Map.of(
                "mensaje", "Registro exitoso. Tu cuenta será revisada por un administrador.",
                "userId", persona.getIdentificador()
        ));
    }

    // ── FORGOT PASSWORD ────────────────────────────────────────────────────────
    record ForgotRequest(@NotBlank @Email String email) {}

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotRequest req) {
        if (!usuarioRepo.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El email no coincide con ningún usuario registrado."));
        }
        try {
            resetService.sendCode(req.email());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "No se pudo enviar el email. Verificá la configuración de correo."));
        }
        return ResponseEntity.ok(Map.of(
                "mensaje", "Código enviado. Revisá tu bandeja de entrada."
        ));
    }

    // ── RESET PASSWORD ─────────────────────────────────────────────────────────
    record ResetRequest(@NotBlank @Email String email, @NotBlank String codigo, @NotBlank @Size(min = 6) String nuevaPassword) {}

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetRequest req) {
        if (!resetService.verifyCode(req.email(), req.codigo())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código inválido o expirado."));
        }

        var usuario = usuarioRepo.findByEmail(req.email()).orElse(null);
        if (usuario == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Usuario no encontrado."));
        }

        usuario.setPassword(encoder.encode(req.nuevaPassword()));
        usuarioRepo.save(usuario);
        resetService.invalidate(req.email());

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(org.springframework.security.core.Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        return usuarioRepo.findByEmail(auth.getName()).map(u -> {
            java.util.Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("email",  u.getEmail());
            resp.put("rol",    u.getRol());
            resp.put("userId", u.getIdentificador());
            resp.put("nombre", u.getPersona() != null ? u.getPersona().getNombre() : "");

            if ("cliente".equals(u.getRol())) {
                clienteRepo.findById(u.getIdentificador())
                    .ifPresent(c -> resp.put("categoria", c.getCategoria()));
            }
            if (!resp.containsKey("categoria")) resp.put("categoria", null);

            return ResponseEntity.ok(resp);
        }).orElse(ResponseEntity.status(404).build());
    }
}
