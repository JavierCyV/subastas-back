-- ================================================================
-- SUBASTAS APP — Schema PostgreSQL (Supabase)
-- Ejecutar en: Supabase Dashboard > SQL Editor
--
-- INSTRUCCIONES:
--   1. Abrir Supabase Dashboard > SQL Editor
--   2. Pegar TODO este archivo
--   3. Click en Run
-- ================================================================

SET search_path TO public;

-- ----------------------------------------------------------------
-- PASO 1: LIMPIAR TODO
-- ----------------------------------------------------------------
DROP FUNCTION IF EXISTS fn_check_fecha_subasta() CASCADE;

DROP TABLE IF EXISTS compras_empresa           CASCADE;
DROP TABLE IF EXISTS metodospago_verificacion  CASCADE;
DROP TABLE IF EXISTS metodospago_garantia      CASCADE;
DROP TABLE IF EXISTS subastas_moneda           CASCADE;
DROP TABLE IF EXISTS pujos_timestamp           CASCADE;
DROP TABLE IF EXISTS victoriaspago             CASCADE;
DROP TABLE IF EXISTS itemscatalogo_subitems    CASCADE;
DROP TABLE IF EXISTS productos_origen          CASCADE;
DROP TABLE IF EXISTS duenios_cuenta            CASCADE;
DROP TABLE IF EXISTS devoluciones              CASCADE;
DROP TABLE IF EXISTS facturas                  CASCADE;
DROP TABLE IF EXISTS productos_artista         CASCADE;
DROP TABLE IF EXISTS productos_ubicacion       CASCADE;
DROP TABLE IF EXISTS depositos                 CASCADE;
DROP TABLE IF EXISTS registros_pendientes      CASCADE;
DROP TABLE IF EXISTS personas_dni              CASCADE;
DROP TABLE IF EXISTS multas                    CASCADE;
DROP TABLE IF EXISTS metodosdepago             CASCADE;
DROP TABLE IF EXISTS usuarios                  CASCADE;
DROP TABLE IF EXISTS registrodesubasta         CASCADE;
DROP TABLE IF EXISTS pujos                     CASCADE;
DROP TABLE IF EXISTS asistentes                CASCADE;
DROP TABLE IF EXISTS itemscatalogo             CASCADE;
DROP TABLE IF EXISTS catalogos                 CASCADE;
DROP TABLE IF EXISTS fotos                     CASCADE;
DROP TABLE IF EXISTS productos                 CASCADE;
DROP TABLE IF EXISTS subastas                  CASCADE;
DROP TABLE IF EXISTS subastadores              CASCADE;
DROP TABLE IF EXISTS duenios                   CASCADE;
DROP TABLE IF EXISTS clientes                  CASCADE;
DROP TABLE IF EXISTS sectores                  CASCADE;
DROP TABLE IF EXISTS seguros                   CASCADE;
DROP TABLE IF EXISTS empleados                 CASCADE;
DROP TABLE IF EXISTS personas                  CASCADE;
DROP TABLE IF EXISTS paises                    CASCADE;

-- ----------------------------------------------------------------
-- PASO 2: CREAR TABLAS (solo PK y CHECK, sin FK por ahora)
-- ----------------------------------------------------------------

CREATE TABLE paises (
    numero       INT          NOT NULL,
    nombre       VARCHAR(250) NOT NULL,
    nombrecorto  VARCHAR(250),
    capital      VARCHAR(250) NOT NULL,
    nacionalidad VARCHAR(250) NOT NULL,
    idiomas      VARCHAR(150) NOT NULL,
    CONSTRAINT pk_paises PRIMARY KEY (numero)
);

CREATE TABLE personas (
    identificador SERIAL      NOT NULL,
    documento     VARCHAR(20) NOT NULL,
    nombre        VARCHAR(150) NOT NULL,
    direccion     VARCHAR(250),
    estado        VARCHAR(15)  CONSTRAINT chkestado CHECK (estado IN ('activo','incativo')),
    foto          BYTEA,
    CONSTRAINT pk_personas PRIMARY KEY (identificador)
);

CREATE TABLE empleados (
    identificador INT          NOT NULL,
    cargo         VARCHAR(100),
    sector        INT,
    CONSTRAINT pk_empleados PRIMARY KEY (identificador)
);

CREATE TABLE sectores (
    identificador     SERIAL       NOT NULL,
    nombresector      VARCHAR(150) NOT NULL,
    codigosector      VARCHAR(10),
    responsablesector INT,
    CONSTRAINT pk_sectores PRIMARY KEY (identificador)
);

CREATE TABLE seguros (
    nropoliza       VARCHAR(30)   NOT NULL,
    compania        VARCHAR(150)  NOT NULL,
    polizacombinada VARCHAR(2)    CONSTRAINT chkpolizacombinada CHECK (polizacombinada IN ('si','no')),
    importe         DECIMAL(18,2) NOT NULL CONSTRAINT chkimporte CHECK (importe > 0),
    CONSTRAINT pk_seguro PRIMARY KEY (nropoliza)
);

CREATE TABLE clientes (
    identificador INT         NOT NULL,
    numeropais    INT,
    admitido      VARCHAR(2)  CONSTRAINT chkadmitido  CHECK (admitido  IN ('si','no')),
    categoria     VARCHAR(10) CONSTRAINT chkcategoria CHECK (categoria IN ('comun','especial','plata','oro','platino')),
    verificador   INT         NOT NULL,
    CONSTRAINT pk_clientes PRIMARY KEY (identificador)
);

CREATE TABLE duenios (
    identificador          INT        NOT NULL,
    numeropais             INT,
    verificacionfinanciera VARCHAR(2) CONSTRAINT chkvf CHECK (verificacionfinanciera IN ('si','no')),
    verificacionjudicial   VARCHAR(2) CONSTRAINT chkvj CHECK (verificacionjudicial   IN ('si','no')),
    calificacionriesgo     INT        CONSTRAINT chkcr CHECK (calificacionriesgo IN (1,2,3,4,5,6)),
    verificador            INT        NOT NULL,
    CONSTRAINT pk_duenios PRIMARY KEY (identificador)
);

CREATE TABLE subastadores (
    identificador INT         NOT NULL,
    matricula     VARCHAR(15),
    region        VARCHAR(50),
    CONSTRAINT pk_subastadores PRIMARY KEY (identificador)
);

CREATE TABLE subastas (
    identificador       SERIAL       NOT NULL,
    fecha               DATE,
    hora                TIME         NOT NULL,
    estado              VARCHAR(10)  CONSTRAINT chkes CHECK (estado IN ('abierta','cerrada')),
    subastador          INT,
    ubicacion           VARCHAR(350),
    capacidadasistentes INT,
    tienedeposito       VARCHAR(2)   CONSTRAINT chktd CHECK (tienedeposito  IN ('si','no')),
    seguridadpropia     VARCHAR(2)   CONSTRAINT chksp CHECK (seguridadpropia IN ('si','no')),
    categoria           VARCHAR(10)  CONSTRAINT chkcs CHECK (categoria IN ('comun','especial','plata','oro','platino')),
    CONSTRAINT pk_subastas PRIMARY KEY (identificador)
);

CREATE TABLE productos (
    identificador       SERIAL        NOT NULL,
    fecha               DATE,
    disponible          VARCHAR(2)    CONSTRAINT chkd CHECK (disponible IN ('si','no')),
    descripcioncatalogo VARCHAR(500)  DEFAULT 'No Posee',
    descripcioncompleta VARCHAR(300),
    revisor             INT,
    duenio              INT,
    seguro              VARCHAR(30),
    -- Solicitud de subasta (flujo de envío por parte del usuario)
    titulo              VARCHAR(200),
    clientesolicitante  INT,
    estado              VARCHAR(25)   DEFAULT 'pendiente',
    preciobasesugerido  DECIMAL(18,2),
    preciobaseoficial   DECIMAL(18,2),
    comisionoficial     DECIMAL(18,2),
    motivorechazo       VARCHAR(1000),
    archivocomprobante  VARCHAR(500),
    declaracionjurada   VARCHAR(2)    DEFAULT 'no' CONSTRAINT chkdj CHECK (declaracionjurada IN ('si','no')),
    fechasolicitud      TIMESTAMP     DEFAULT NOW(),
    CONSTRAINT pk_productos PRIMARY KEY (identificador)
);

CREATE TABLE fotos (
    identificador SERIAL NOT NULL,
    producto      INT    NOT NULL,
    foto          BYTEA  NOT NULL,
    CONSTRAINT pk_fotos PRIMARY KEY (identificador)
);

CREATE TABLE catalogos (
    identificador SERIAL       NOT NULL,
    descripcion   VARCHAR(250) NOT NULL,
    subasta       INT,
    responsable   INT          NOT NULL,
    CONSTRAINT pk_catalogos PRIMARY KEY (identificador)
);

CREATE TABLE itemscatalogo (
    identificador SERIAL        NOT NULL,
    catalogo      INT           NOT NULL,
    producto      INT           NOT NULL,
    preciobase    DECIMAL(18,2) NOT NULL CONSTRAINT chkpb CHECK (preciobase > 0.01),
    comision      DECIMAL(18,2) NOT NULL CONSTRAINT chkc  CHECK (comision   > 0.01),
    subastado     VARCHAR(2)    CONSTRAINT chks CHECK (subastado IN ('si','no')),
    CONSTRAINT pk_itemscatalogo PRIMARY KEY (identificador)
);

CREATE TABLE asistentes (
    identificador SERIAL NOT NULL,
    numeropostor  INT    NOT NULL,
    cliente       INT    NOT NULL,
    subasta       INT    NOT NULL,
    CONSTRAINT pk_asistentes PRIMARY KEY (identificador)
);

CREATE TABLE pujos (
    identificador SERIAL        NOT NULL,
    asistente     INT           NOT NULL,
    item          INT           NOT NULL,
    importe       DECIMAL(18,2) NOT NULL CONSTRAINT chki CHECK (importe > 0.01),
    ganador       VARCHAR(2)    DEFAULT 'no' CONSTRAINT chkg CHECK (ganador IN ('si','no')),
    CONSTRAINT pk_pujos PRIMARY KEY (identificador)
);

CREATE TABLE registrodesubasta (
    identificador SERIAL        NOT NULL,
    subasta       INT           NOT NULL,
    duenio        INT           NOT NULL,
    producto      INT           NOT NULL,
    cliente       INT           NOT NULL,
    importe       DECIMAL(18,2) NOT NULL CONSTRAINT chkimportepagado  CHECK (importe  > 0.01),
    comision      DECIMAL(18,2) NOT NULL CONSTRAINT chkcomisionpagada CHECK (comision > 0.01),
    CONSTRAINT pk_registrodesubasta PRIMARY KEY (identificador)
);

CREATE TABLE usuarios (
    identificador INT          NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    rol           VARCHAR(20)  NOT NULL CONSTRAINT chkrol      CHECK (rol      IN ('cliente','duenio','empleado')),
    aprobado      VARCHAR(2)   NOT NULL DEFAULT 'no' CONSTRAINT chkaprobado CHECK (aprobado IN ('si','no')),
    fecharegistro TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_usuarios       PRIMARY KEY (identificador),
    CONSTRAINT uq_usuarios_email UNIQUE (email)
);

CREATE TABLE metodosdepago (
    identificador SERIAL       NOT NULL,
    cliente       INT          NOT NULL,
    tipo          VARCHAR(20)  NOT NULL CONSTRAINT chktipopago CHECK (tipo IN ('transferencia','cheque','efectivo')),
    detalle       VARCHAR(500) NOT NULL,
    activo        VARCHAR(2)   NOT NULL DEFAULT 'si' CONSTRAINT chkactivo CHECK (activo IN ('si','no')),
    fechaalta     TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_metodosdepago PRIMARY KEY (identificador)
);

-- ----------------------------------------------------------------
-- PASO 2b: TABLAS NUEVAS (multas, victorias, timestamps, moneda, etc.)
-- ----------------------------------------------------------------

CREATE TABLE IF NOT EXISTS personas_dni (
    persona_id INT NOT NULL,
    foto_frente VARCHAR(500),
    foto_dorso VARCHAR(500),
    CONSTRAINT pk_personas_dni PRIMARY KEY (persona_id)
);

CREATE TABLE IF NOT EXISTS registros_pendientes (
    identificador SERIAL NOT NULL,
    email VARCHAR(150) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    documento VARCHAR(20) NOT NULL,
    direccion VARCHAR(250),
    telefono VARCHAR(50),
    pais INT,
    foto_dni_frente VARCHAR(500),
    foto_dni_dorso VARCHAR(500),
    rol VARCHAR(20) NOT NULL,
    codigo_completar VARCHAR(6),
    codigo_expiracion TIMESTAMP,
    creado TIMESTAMP DEFAULT NOW(),
    CONSTRAINT pk_registros_pendientes PRIMARY KEY (identificador)
);

CREATE TABLE IF NOT EXISTS depositos (
    identificador SERIAL NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(250),
    CONSTRAINT pk_depositos PRIMARY KEY (identificador)
);

CREATE TABLE IF NOT EXISTS productos_ubicacion (
    producto INT NOT NULL,
    deposito INT NOT NULL,
    ubicacion_detalle VARCHAR(500),
    CONSTRAINT pk_productos_ubicacion PRIMARY KEY (producto)
);

CREATE TABLE IF NOT EXISTS productos_artista (
    producto INT NOT NULL,
    artista VARCHAR(200),
    fecha_obra DATE,
    historia TEXT,
    duenios_anteriores TEXT,
    CONSTRAINT pk_productos_artista PRIMARY KEY (producto)
);

CREATE TABLE IF NOT EXISTS facturas (
    identificador SERIAL NOT NULL,
    registro INT NOT NULL,
    cliente INT NOT NULL,
    importe_pujado DECIMAL(18,2) NOT NULL,
    comision DECIMAL(18,2) NOT NULL,
    costo_envio DECIMAL(18,2) DEFAULT 0,
    total DECIMAL(18,2) NOT NULL,
    tipo_entrega VARCHAR(20) CONSTRAINT chkfe CHECK (tipo_entrega IN ('envio','retiro_personal')),
    direccion_envio VARCHAR(250),
    con_seguro VARCHAR(2) DEFAULT 'si' CONSTRAINT chkfcs CHECK (con_seguro IN ('si','no')),
    emitida TIMESTAMP DEFAULT NOW(),
    CONSTRAINT pk_facturas PRIMARY KEY (identificador)
);

CREATE TABLE IF NOT EXISTS devoluciones (
    identificador SERIAL NOT NULL,
    producto INT NOT NULL,
    motivo VARCHAR(1000) NOT NULL,
    cargo DECIMAL(18,2),
    fecha TIMESTAMP DEFAULT NOW(),
    estado VARCHAR(20) DEFAULT 'pendiente',
    CONSTRAINT pk_devoluciones PRIMARY KEY (identificador)
);

CREATE TABLE IF NOT EXISTS duenios_cuenta (
    duenio INT NOT NULL,
    banco VARCHAR(200),
    tipo_cuenta VARCHAR(50),
    numero_cuenta VARCHAR(100),
    moneda VARCHAR(3) DEFAULT 'ARS',
    es_exterior VARCHAR(2) DEFAULT 'no' CONSTRAINT chkdc CHECK (es_exterior IN ('si','no')),
    saldo NUMERIC(15,2) DEFAULT 0.00,
    CONSTRAINT pk_duenios_cuenta PRIMARY KEY (duenio)
);

CREATE TABLE IF NOT EXISTS productos_origen (
    producto INT NOT NULL,
    tipo_documento VARCHAR(100),
    archivo VARCHAR(500),
    verificado VARCHAR(2) DEFAULT 'no' CONSTRAINT chkpo CHECK (verificado IN ('si','no')),
    CONSTRAINT pk_productos_origen PRIMARY KEY (producto)
);

CREATE TABLE IF NOT EXISTS itemscatalogo_subitems (
    identificador SERIAL NOT NULL,
    item_catalogo INT NOT NULL,
    descripcion VARCHAR(300) NOT NULL,
    cantidad INT DEFAULT 1,
    CONSTRAINT pk_itemscatalogo_subitems PRIMARY KEY (identificador)
);

CREATE TABLE IF NOT EXISTS multas (
    identificador SERIAL NOT NULL,
    registro INT REFERENCES registrodesubasta(identificador),
    cliente INT REFERENCES clientes(identificador),
    importe DECIMAL(18,2),
    motivo VARCHAR(500),
    pagada VARCHAR(2) DEFAULT 'no',
    fechamulta TIMESTAMP DEFAULT NOW(),
    CONSTRAINT pk_multas PRIMARY KEY (identificador)
);

CREATE TABLE IF NOT EXISTS victoriaspago (
    identificador SERIAL NOT NULL,
    registro INT REFERENCES registrodesubasta(identificador),
    cliente INT REFERENCES clientes(identificador),
    importe DECIMAL(18,2),
    fechavictoria TIMESTAMP DEFAULT NOW(),
    pagado VARCHAR(2) DEFAULT 'no',
    metodopago INT REFERENCES metodosdepago(identificador),
    CONSTRAINT pk_victoriaspago PRIMARY KEY (identificador)
);

CREATE TABLE IF NOT EXISTS pujos_timestamp (
    pujo_id INT NOT NULL REFERENCES pujos(identificador),
    fecha_pujo TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_pujos_timestamp PRIMARY KEY (pujo_id)
);

CREATE TABLE IF NOT EXISTS subastas_moneda (
    subasta_id INT NOT NULL REFERENCES subastas(identificador),
    moneda VARCHAR(3) NOT NULL DEFAULT 'ARS' CHECK (moneda IN ('ARS','USD')),
    CONSTRAINT pk_subastas_moneda PRIMARY KEY (subasta_id)
);

CREATE TABLE IF NOT EXISTS metodospago_garantia (
    metodopago_id INT NOT NULL REFERENCES metodosdepago(identificador),
    monto_garantia DECIMAL(18,2) NOT NULL CHECK (monto_garantia > 0),
    CONSTRAINT pk_metodospago_garantia PRIMARY KEY (metodopago_id)
);

CREATE TABLE IF NOT EXISTS metodospago_verificacion (
    metodopago_id INT NOT NULL REFERENCES metodosdepago(identificador),
    verificado VARCHAR(2) NOT NULL DEFAULT 'no' CHECK (verificado IN ('si','no')),
    CONSTRAINT pk_metodospago_verificacion PRIMARY KEY (metodopago_id)
);

CREATE TABLE IF NOT EXISTS compras_empresa (
    identificador SERIAL NOT NULL,
    subasta INT NOT NULL REFERENCES subastas(identificador),
    producto INT NOT NULL REFERENCES productos(identificador),
    item INT NOT NULL REFERENCES itemscatalogo(identificador),
    importe DECIMAL(18,2) NOT NULL,
    fecha TIMESTAMP DEFAULT NOW(),
    CONSTRAINT pk_compras_empresa PRIMARY KEY (identificador)
);

-- ----------------------------------------------------------------
-- PASO 3: AGREGAR FOREIGN KEYS (todas las tablas ya existen)
-- ----------------------------------------------------------------

ALTER TABLE sectores        ADD CONSTRAINT fk_sectores_empleados       FOREIGN KEY (responsablesector) REFERENCES empleados (identificador);

ALTER TABLE clientes        ADD CONSTRAINT fk_clientes_personas         FOREIGN KEY (identificador)     REFERENCES personas  (identificador);
ALTER TABLE clientes        ADD CONSTRAINT fk_clientes_empleados        FOREIGN KEY (verificador)       REFERENCES empleados (identificador);
ALTER TABLE clientes        ADD CONSTRAINT fk_clientes_paises           FOREIGN KEY (numeropais)        REFERENCES paises    (numero);

ALTER TABLE duenios         ADD CONSTRAINT fk_duenios_personas          FOREIGN KEY (identificador)     REFERENCES personas  (identificador);
ALTER TABLE duenios         ADD CONSTRAINT fk_duenios_empleados         FOREIGN KEY (verificador)       REFERENCES empleados (identificador);

ALTER TABLE subastadores    ADD CONSTRAINT fk_subastadores_personas     FOREIGN KEY (identificador)     REFERENCES personas  (identificador);

ALTER TABLE subastas        ADD CONSTRAINT fk_subastas_subastadores     FOREIGN KEY (subastador)        REFERENCES subastadores (identificador);

ALTER TABLE productos       ADD CONSTRAINT fk_productos_empleados       FOREIGN KEY (revisor)              REFERENCES empleados (identificador);
ALTER TABLE productos       ADD CONSTRAINT fk_productos_duenios         FOREIGN KEY (duenio)               REFERENCES duenios   (identificador);
ALTER TABLE productos       ADD CONSTRAINT fk_productos_clientes_sol    FOREIGN KEY (clientesolicitante)   REFERENCES clientes  (identificador);

ALTER TABLE fotos           ADD CONSTRAINT fk_fotos_productos           FOREIGN KEY (producto)          REFERENCES productos (identificador);

ALTER TABLE catalogos       ADD CONSTRAINT fk_catalogos_empleados       FOREIGN KEY (responsable)       REFERENCES empleados (identificador);
ALTER TABLE catalogos       ADD CONSTRAINT fk_catalogos_subastas        FOREIGN KEY (subasta)           REFERENCES subastas  (identificador);

ALTER TABLE itemscatalogo   ADD CONSTRAINT fk_itemscatalogo_catalogos   FOREIGN KEY (catalogo)          REFERENCES catalogos    (identificador);
ALTER TABLE itemscatalogo   ADD CONSTRAINT fk_itemscatalogo_productos   FOREIGN KEY (producto)          REFERENCES productos    (identificador);

ALTER TABLE asistentes      ADD CONSTRAINT fk_asistentes_clientes       FOREIGN KEY (cliente)           REFERENCES clientes  (identificador);
ALTER TABLE asistentes      ADD CONSTRAINT fk_asistentes_subastas       FOREIGN KEY (subasta)           REFERENCES subastas  (identificador);

ALTER TABLE pujos           ADD CONSTRAINT fk_pujos_asistentes          FOREIGN KEY (asistente)         REFERENCES asistentes   (identificador);
ALTER TABLE pujos           ADD CONSTRAINT fk_pujos_itemscatalogo       FOREIGN KEY (item)              REFERENCES itemscatalogo (identificador);

ALTER TABLE registrodesubasta ADD CONSTRAINT fk_registrodesubasta_subastas  FOREIGN KEY (subasta)  REFERENCES subastas  (identificador);
ALTER TABLE registrodesubasta ADD CONSTRAINT fk_registrodesubasta_duenios   FOREIGN KEY (duenio)   REFERENCES duenios   (identificador);
ALTER TABLE registrodesubasta ADD CONSTRAINT fk_registrodesubasta_productos FOREIGN KEY (producto) REFERENCES productos (identificador);
ALTER TABLE registrodesubasta ADD CONSTRAINT fk_registrodesubasta_clientes  FOREIGN KEY (cliente)  REFERENCES clientes  (identificador);

ALTER TABLE usuarios        ADD CONSTRAINT fk_usuarios_personas         FOREIGN KEY (identificador)     REFERENCES personas  (identificador);

ALTER TABLE metodosdepago   ADD CONSTRAINT fk_metodosdepago_clientes    FOREIGN KEY (cliente)           REFERENCES clientes  (identificador);

ALTER TABLE personas_dni            ADD CONSTRAINT fk_pd_personas          FOREIGN KEY (persona_id)  REFERENCES personas  (identificador);
ALTER TABLE productos_ubicacion     ADD CONSTRAINT fk_pu_productos         FOREIGN KEY (producto)   REFERENCES productos (identificador);
ALTER TABLE productos_ubicacion     ADD CONSTRAINT fk_pu_depositos         FOREIGN KEY (deposito)   REFERENCES depositos (identificador);
ALTER TABLE productos_artista       ADD CONSTRAINT fk_pa_productos         FOREIGN KEY (producto)   REFERENCES productos (identificador);
ALTER TABLE facturas                ADD CONSTRAINT fk_fact_registro        FOREIGN KEY (registro)   REFERENCES registrodesubasta (identificador);
ALTER TABLE facturas                ADD CONSTRAINT fk_fact_cliente         FOREIGN KEY (cliente)    REFERENCES clientes (identificador);
ALTER TABLE devoluciones            ADD CONSTRAINT fk_dev_productos        FOREIGN KEY (producto)   REFERENCES productos (identificador);
ALTER TABLE duenios_cuenta          ADD CONSTRAINT fk_dc_duenios           FOREIGN KEY (duenio)     REFERENCES duenios (identificador);
ALTER TABLE productos_origen        ADD CONSTRAINT fk_po_productos         FOREIGN KEY (producto)   REFERENCES productos (identificador);
ALTER TABLE itemscatalogo_subitems  ADD CONSTRAINT fk_ics_itemscatalogo   FOREIGN KEY (item_catalogo) REFERENCES itemscatalogo (identificador);

-- ----------------------------------------------------------------
-- PASO 4: TRIGGER para validar fecha de subasta
-- ----------------------------------------------------------------

CREATE OR REPLACE FUNCTION fn_check_fecha_subasta()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.fecha IS NOT NULL AND NEW.fecha <= (CURRENT_DATE + INTERVAL '10 days')::date THEN
        RAISE EXCEPTION 'La fecha de la subasta debe ser al menos 10 dias en el futuro';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_fecha_subasta
BEFORE INSERT OR UPDATE ON subastas
FOR EACH ROW EXECUTE FUNCTION fn_check_fecha_subasta();

-- ----------------------------------------------------------------
-- PASO 5: DATOS INICIALES
-- ----------------------------------------------------------------

INSERT INTO paises (numero, nombre, nombrecorto, capital, nacionalidad, idiomas)
VALUES (54, 'Argentina', 'ARG', 'Buenos Aires', 'Argentino/a', 'Español');

-- Admin: email=admin@subastas.com  password=admin123
WITH persona_insertada AS (
    INSERT INTO personas (documento, nombre, estado)
    VALUES ('00000001', 'Administrador Sistema', 'activo')
    RETURNING identificador
),
empleado_insertado AS (
    INSERT INTO empleados (identificador, cargo)
    SELECT identificador, 'Administrador' FROM persona_insertada
    RETURNING identificador
)
INSERT INTO usuarios (identificador, email, password, rol, aprobado)
SELECT identificador,
       'admin@subastas.com',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8RqNmu/LKvOZaIUeWe',
       'empleado',
       'si'
FROM empleado_insertado;
