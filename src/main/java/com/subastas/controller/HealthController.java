package com.subastas.controller;

import jakarta.annotation.PostConstruct;
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

    private void crearTablasSiNoExisten() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS multas (identificador SERIAL NOT NULL, registro INT REFERENCES registrodesubasta(identificador), cliente INT REFERENCES clientes(identificador), importe DECIMAL(18,2), motivo VARCHAR(500), pagada VARCHAR(2) DEFAULT 'no', fechamulta TIMESTAMP DEFAULT NOW(), CONSTRAINT pk_multas PRIMARY KEY (identificador))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS victoriaspago (identificador SERIAL NOT NULL, registro INT REFERENCES registrodesubasta(identificador), cliente INT REFERENCES clientes(identificador), importe DECIMAL(18,2), fechavictoria TIMESTAMP DEFAULT NOW(), pagado VARCHAR(2) DEFAULT 'no', metodopago INT REFERENCES metodosdepago(identificador), CONSTRAINT pk_victoriaspago PRIMARY KEY (identificador))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS pujos_timestamp (pujo_id INT NOT NULL REFERENCES pujos(identificador), fecha_pujo TIMESTAMP NOT NULL DEFAULT NOW(), CONSTRAINT pk_pujos_timestamp PRIMARY KEY (pujo_id))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS subastas_moneda (subasta_id INT NOT NULL REFERENCES subastas(identificador), moneda VARCHAR(3) NOT NULL DEFAULT 'ARS' CHECK (moneda IN ('ARS','USD')), CONSTRAINT pk_subastas_moneda PRIMARY KEY (subasta_id))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS metodospago_garantia (metodopago_id INT NOT NULL REFERENCES metodosdepago(identificador), monto_garantia DECIMAL(18,2) NOT NULL CHECK (monto_garantia > 0), CONSTRAINT pk_metodospago_garantia PRIMARY KEY (metodopago_id))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS metodospago_verificacion (metodopago_id INT NOT NULL REFERENCES metodosdepago(identificador), verificado VARCHAR(2) NOT NULL DEFAULT 'no' CHECK (verificado IN ('si','no')), CONSTRAINT pk_metodospago_verificacion PRIMARY KEY (metodopago_id))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS compras_empresa (identificador SERIAL NOT NULL, subasta INT NOT NULL REFERENCES subastas(identificador), producto INT NOT NULL REFERENCES productos(identificador), item INT NOT NULL REFERENCES itemscatalogo(identificador), importe DECIMAL(18,2) NOT NULL, fecha TIMESTAMP DEFAULT NOW(), CONSTRAINT pk_compras_empresa PRIMARY KEY (identificador))");
    }

    @PostConstruct
    public void init() {
        try {
            crearTablasSiNoExisten();
        } catch (Exception ignored) {}
    }

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

            crearTablasSiNoExisten();

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
