package com.subastas.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS titulo VARCHAR(200)");
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS clientesolicitante INT");
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS estado VARCHAR(25) DEFAULT 'pendiente'");
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS preciobasesugerido DECIMAL(18,2)");
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS preciobaseoficial DECIMAL(18,2)");
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS comisionoficial DECIMAL(18,2)");
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS motivorechazo VARCHAR(1000)");
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS archivocomprobante VARCHAR(500)");
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS declaracionjurada VARCHAR(2) DEFAULT 'no'");
            jdbcTemplate.execute("ALTER TABLE productos ADD COLUMN IF NOT EXISTS fechasolicitud TIMESTAMP DEFAULT NOW()");
            jdbcTemplate.execute("ALTER TABLE productos ALTER COLUMN revisor DROP NOT NULL");
            jdbcTemplate.execute("ALTER TABLE productos ALTER COLUMN duenio DROP NOT NULL");
            return ResponseEntity.ok(Map.of(
                    "status", "ok (db patched)",
                    "timestamp", Instant.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "status", "error patching db: " + e.getMessage(),
                    "timestamp", Instant.now().toString()
            ));
        }
    }
}
