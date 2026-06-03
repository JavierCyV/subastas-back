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

DROP TABLE IF EXISTS metodosdepago      CASCADE;
DROP TABLE IF EXISTS usuarios           CASCADE;
DROP TABLE IF EXISTS registrodesubasta  CASCADE;
DROP TABLE IF EXISTS pujos              CASCADE;
DROP TABLE IF EXISTS asistentes         CASCADE;
DROP TABLE IF EXISTS itemscatalogo      CASCADE;
DROP TABLE IF EXISTS catalogos          CASCADE;
DROP TABLE IF EXISTS fotos              CASCADE;
DROP TABLE IF EXISTS productos          CASCADE;
DROP TABLE IF EXISTS subastas           CASCADE;
DROP TABLE IF EXISTS subastadores       CASCADE;
DROP TABLE IF EXISTS duenios            CASCADE;
DROP TABLE IF EXISTS clientes           CASCADE;
DROP TABLE IF EXISTS sectores           CASCADE;
DROP TABLE IF EXISTS seguros            CASCADE;
DROP TABLE IF EXISTS empleados          CASCADE;
DROP TABLE IF EXISTS personas           CASCADE;
DROP TABLE IF EXISTS paises             CASCADE;

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
    identificador       SERIAL       NOT NULL,
    fecha               DATE,
    disponible          VARCHAR(2)   CONSTRAINT chkd CHECK (disponible IN ('si','no')),
    descripcioncatalogo VARCHAR(500) DEFAULT 'No Posee',
    descripcioncompleta VARCHAR(300) NOT NULL,
    revisor             INT          NOT NULL,
    duenio              INT          NOT NULL,
    seguro              VARCHAR(30),
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

ALTER TABLE productos       ADD CONSTRAINT fk_productos_empleados       FOREIGN KEY (revisor)           REFERENCES empleados (identificador);
ALTER TABLE productos       ADD CONSTRAINT fk_productos_duenios         FOREIGN KEY (duenio)            REFERENCES duenios   (identificador);

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
