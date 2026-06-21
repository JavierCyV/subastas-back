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
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS personas_dni (persona_id INT NOT NULL REFERENCES personas(identificador), foto_frente VARCHAR(500), foto_dorso VARCHAR(500), CONSTRAINT pk_personas_dni PRIMARY KEY (persona_id))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS registros_pendientes (identificador SERIAL NOT NULL, email VARCHAR(150) NOT NULL, nombre VARCHAR(150) NOT NULL, documento VARCHAR(20) NOT NULL, direccion VARCHAR(250), telefono VARCHAR(50), pais INT, foto_dni_frente VARCHAR(500), foto_dni_dorso VARCHAR(500), rol VARCHAR(20) NOT NULL, codigo_completar VARCHAR(6), codigo_expiracion TIMESTAMP, creado TIMESTAMP DEFAULT NOW(), CONSTRAINT pk_registros_pendientes PRIMARY KEY (identificador))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS depositos (identificador SERIAL NOT NULL, nombre VARCHAR(150) NOT NULL, direccion VARCHAR(250), CONSTRAINT pk_depositos PRIMARY KEY (identificador))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS productos_ubicacion (producto INT NOT NULL REFERENCES productos(identificador), deposito INT NOT NULL REFERENCES depositos(identificador), ubicacion_detalle VARCHAR(500), CONSTRAINT pk_productos_ubicacion PRIMARY KEY (producto))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS productos_artista (producto INT NOT NULL REFERENCES productos(identificador), artista VARCHAR(200), fecha_obra DATE, historia TEXT, duenios_anteriores TEXT, CONSTRAINT pk_productos_artista PRIMARY KEY (producto))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS facturas (identificador SERIAL NOT NULL, registro INT NOT NULL REFERENCES registrodesubasta(identificador), cliente INT NOT NULL REFERENCES clientes(identificador), importe_pujado DECIMAL(18,2) NOT NULL, comision DECIMAL(18,2) NOT NULL, costo_envio DECIMAL(18,2) DEFAULT 0, total DECIMAL(18,2) NOT NULL, tipo_entrega VARCHAR(20) CONSTRAINT chkfe CHECK (tipo_entrega IN ('envio','retiro_personal')), direccion_envio VARCHAR(250), con_seguro VARCHAR(2) DEFAULT 'si' CONSTRAINT chkfcs CHECK (con_seguro IN ('si','no')), emitida TIMESTAMP DEFAULT NOW(), CONSTRAINT pk_facturas PRIMARY KEY (identificador))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS devoluciones (identificador SERIAL NOT NULL, producto INT NOT NULL REFERENCES productos(identificador), motivo VARCHAR(1000) NOT NULL, cargo DECIMAL(18,2), fecha TIMESTAMP DEFAULT NOW(), estado VARCHAR(20) DEFAULT 'pendiente', CONSTRAINT pk_devoluciones PRIMARY KEY (identificador))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS duenios_cuenta (duenio INT NOT NULL REFERENCES duenios(identificador), banco VARCHAR(200), tipo_cuenta VARCHAR(50), numero_cuenta VARCHAR(100), moneda VARCHAR(3) DEFAULT 'ARS', es_exterior VARCHAR(2) DEFAULT 'no' CONSTRAINT chkdc CHECK (es_exterior IN ('si','no')), CONSTRAINT pk_duenios_cuenta PRIMARY KEY (duenio))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS productos_origen (producto INT NOT NULL REFERENCES productos(identificador), tipo_documento VARCHAR(100), archivo VARCHAR(500), verificado VARCHAR(2) DEFAULT 'no' CONSTRAINT chkpo CHECK (verificado IN ('si','no')), CONSTRAINT pk_productos_origen PRIMARY KEY (producto))");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS itemscatalogo_subitems (identificador SERIAL NOT NULL, item_catalogo INT NOT NULL REFERENCES itemscatalogo(identificador), descripcion VARCHAR(300) NOT NULL, cantidad INT DEFAULT 1, CONSTRAINT pk_itemscatalogo_subitems PRIMARY KEY (identificador))");
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
