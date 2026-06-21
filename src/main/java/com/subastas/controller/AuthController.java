package com.subastas.controller;

import com.subastas.entity.Cliente;
import com.subastas.entity.Duenio;
import com.subastas.entity.Persona;
import com.subastas.entity.PersonaDni;
import com.subastas.entity.RegistroPendiente;
import com.subastas.entity.Usuario;
import com.subastas.repository.ClienteRepository;
import com.subastas.repository.DuenioRepository;
import com.subastas.repository.PersonaDniRepository;
import com.subastas.repository.PersonaRepository;
import com.subastas.repository.RegistroPendienteRepository;
import com.subastas.repository.UsuarioRepository;
import com.subastas.security.JwtService;
import com.subastas.service.PasswordResetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final RegistroPendienteRepository registroPendienteRepo;
    private final PersonaDniRepository personaDniRepo;
    private final JavaMailSender mailSender;

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

    // ── PRE-REGISTRO (2 ETAPAS) ────────────────────────────────────────────────
    record PreRegistroRequest(
        @NotBlank String nombre,
        @NotBlank String documento,
        String direccion,
        String telefono,
        Integer pais,
        String fotoFrenteBase64,
        String fotoDorsoBase64,
        @NotBlank @Email String email,
        @NotBlank String rol
    ) {}

    @PostMapping("/pre-register")
    public ResponseEntity<?> preRegister(@Valid @RequestBody PreRegistroRequest req) {
        if (usuarioRepo.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El email ya está registrado"));
        }
        if (registroPendienteRepo.findByEmail(req.email()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ya existe un preregistro con ese email"));
        }

        String fotoFrente = null;
        String fotoDorso = null;
        try {
            if (req.fotoFrenteBase64() != null && !req.fotoFrenteBase64().isBlank()) {
                String payload = req.fotoFrenteBase64().contains(",") ? req.fotoFrenteBase64().split(",")[1] : req.fotoFrenteBase64();
                byte[] bytes = java.util.Base64.getDecoder().decode(payload.trim());
                String filename = "dni_frente_" + java.util.UUID.randomUUID() + ".jpg";
                java.nio.file.Files.write(java.nio.file.Paths.get("uploads", filename), bytes);
                fotoFrente = "/uploads/" + filename;
            }
            if (req.fotoDorsoBase64() != null && !req.fotoDorsoBase64().isBlank()) {
                String payload = req.fotoDorsoBase64().contains(",") ? req.fotoDorsoBase64().split(",")[1] : req.fotoDorsoBase64();
                byte[] bytes = java.util.Base64.getDecoder().decode(payload.trim());
                String filename = "dni_dorso_" + java.util.UUID.randomUUID() + ".jpg";
                java.nio.file.Files.write(java.nio.file.Paths.get("uploads", filename), bytes);
                fotoDorso = "/uploads/" + filename;
            }
        } catch (Exception ignored) {}

        RegistroPendiente rp = new RegistroPendiente();
        rp.setEmail(req.email());
        rp.setNombre(req.nombre());
        rp.setDocumento(req.documento());
        rp.setDireccion(req.direccion());
        rp.setTelefono(req.telefono());
        rp.setPais(req.pais());
        rp.setFotoDniFrente(fotoFrente);
        rp.setFotoDniDorso(fotoDorso);
        rp.setRol(req.rol());

        // Generar código de validación automáticamente
        java.util.Random random = new java.util.Random();
        String codigo = String.format("%06d", random.nextInt(1000000));
        rp.setCodigoCompletar(codigo);
        rp.setCodigoExpiracion(java.time.LocalDateTime.now().plusHours(48));

        rp = registroPendienteRepo.save(rp);

        // Enviar mail inmediatamente
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(rp.getEmail());
            msg.setSubject("Completá tu registro — Subastas");
            msg.setText(
                "Hola " + rp.getNombre() + ",\n\n" +
                "Tu solicitud fue recibida. Usá el siguiente código para verificar tu cuenta y completar tu registro:\n\n" +
                "Código: " + codigo + "\n\n" +
                "El código expira en 48 horas.\n\n" +
                "Equipo Subastas"
            );
            mailSender.send(msg);
        } catch (Exception ignored) {}

        return ResponseEntity.status(201).body(Map.of(
            "mensaje", "Preregistro exitoso. Código de validación generado y enviado.",
            "preRegistroId", rp.getIdentificador(),
            "codigo", codigo
        ));
    }

    // ── COMPLETAR REGISTRO (2ª etapa) ─────────────────────────────────────────
    record CompletarRegistroRequest(
        @NotBlank String codigo,
        @NotBlank @Size(min = 6) String password
    ) {}

    @PostMapping("/complete-registration")
    public ResponseEntity<?> completeRegistration(@Valid @RequestBody CompletarRegistroRequest req) {
        var pendiente = registroPendienteRepo.findByCodigoCompletar(req.codigo()).orElse(null);
        if (pendiente == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Código inválido"));
        }
        if (pendiente.getCodigoExpiracion() != null && java.time.LocalDateTime.now().isAfter(pendiente.getCodigoExpiracion())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El código ha expirado"));
        }

        if (usuarioRepo.existsByEmail(pendiente.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El email ya está registrado"));
        }

        Persona persona = new Persona();
        persona.setDocumento(pendiente.getDocumento());
        persona.setNombre(pendiente.getNombre());
        persona.setDireccion(pendiente.getDireccion());
        persona.setEstado("activo");
        persona = personaRepo.save(persona);

        if ("cliente".equals(pendiente.getRol())) {
            Cliente c = new Cliente();
            c.setIdentificador(persona.getIdentificador());
            c.setCategoria("comun");
            c.setVerificador(1);
            clienteRepo.save(c);
        } else if ("duenio".equals(pendiente.getRol())) {
            Duenio d = new Duenio();
            d.setIdentificador(persona.getIdentificador());
            d.setVerificador(1);
            duenioRepo.save(d);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol inválido"));
        }

        // Guardar fotos DNI si se subieron en preregistro
        if (pendiente.getFotoDniFrente() != null || pendiente.getFotoDniDorso() != null) {
            PersonaDni pd = new PersonaDni();
            pd.setPersonaId(persona.getIdentificador());
            pd.setFotoFrente(pendiente.getFotoDniFrente());
            pd.setFotoDorso(pendiente.getFotoDniDorso());
            personaDniRepo.save(pd);
        }

        Usuario usuario = new Usuario();
        usuario.setIdentificador(persona.getIdentificador());
        usuario.setEmail(pendiente.getEmail());
        usuario.setPassword(encoder.encode(req.password()));
        usuario.setRol(pendiente.getRol());
        usuario.setAprobado("no");
        usuarioRepo.save(usuario);

        registroPendienteRepo.delete(pendiente);

        return ResponseEntity.status(201).body(Map.of(
            "mensaje", "Registro completado. Tu cuenta será revisada por un administrador.",
            "userId", persona.getIdentificador()
        ));
    }

    // ── REGISTRO DIRECTO (original, con fotos DNI opcionales) ──────────────────
    record RegisterRequest(
        @NotBlank String nombre,
        @NotBlank String documento,
        String direccion,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String rol,
        String fotoFrenteBase64,
        String fotoDorsoBase64
    ) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (usuarioRepo.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "El email ya está registrado"));
        }

        Persona persona = new Persona();
        persona.setDocumento(req.documento());
        persona.setNombre(req.nombre());
        persona.setDireccion(req.direccion());
        persona.setEstado("activo");
        persona = personaRepo.save(persona);

        if ("cliente".equals(req.rol())) {
            Cliente c = new Cliente();
            c.setIdentificador(persona.getIdentificador());
            c.setCategoria("comun");
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

        // Guardar fotos DNI
        String fotoFrente = null;
        String fotoDorso = null;
        try {
            if (req.fotoFrenteBase64() != null && !req.fotoFrenteBase64().isBlank()) {
                String payload = req.fotoFrenteBase64().contains(",") ? req.fotoFrenteBase64().split(",")[1] : req.fotoFrenteBase64();
                byte[] bytes = java.util.Base64.getDecoder().decode(payload.trim());
                String filename = "dni_frente_" + java.util.UUID.randomUUID() + ".jpg";
                java.nio.file.Files.write(java.nio.file.Paths.get("uploads", filename), bytes);
                fotoFrente = "/uploads/" + filename;
            }
            if (req.fotoDorsoBase64() != null && !req.fotoDorsoBase64().isBlank()) {
                String payload = req.fotoDorsoBase64().contains(",") ? req.fotoDorsoBase64().split(",")[1] : req.fotoDorsoBase64();
                byte[] bytes = java.util.Base64.getDecoder().decode(payload.trim());
                String filename = "dni_dorso_" + java.util.UUID.randomUUID() + ".jpg";
                java.nio.file.Files.write(java.nio.file.Paths.get("uploads", filename), bytes);
                fotoDorso = "/uploads/" + filename;
            }
        } catch (Exception ignored) {}

        if (fotoFrente != null || fotoDorso != null) {
            PersonaDni pd = new PersonaDni();
            pd.setPersonaId(persona.getIdentificador());
            pd.setFotoFrente(fotoFrente);
            pd.setFotoDorso(fotoDorso);
            personaDniRepo.save(pd);
        }

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
