-- ============================================================
-- V15: Modulo Inventario (RF-27 a RF-30)
--
-- Cierra el schema "inventario" que ADR-003 dejo reservado como diseno a
-- futuro ("Control de uniformes, balones, implementos deportivos y
-- asignaciones"). Stock por cantidad agregada (no serializado por unidad
-- individual): cada articulo tiene un stock_actual que los movimientos y
-- las asignaciones ajustan.
--
-- articulos: catalogo (uniformes, balones, implementos, otro).
-- movimientos_stock: ledger de entradas/salidas/ajustes, independiente
--   de a quien se le entrega algo.
-- asignaciones: entrega/devolucion de un articulo a un estudiante o
--   entrenador (resta stock al asignar, lo repone al devolver).
-- ============================================================

CREATE SCHEMA IF NOT EXISTS inventario;

CREATE OR REPLACE FUNCTION inventario.set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ------------------------------------------------------------
-- Catalogo de articulos
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventario.articulos (
    id_articulo BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('UNIFORME', 'BALON', 'IMPLEMENTO', 'OTRO')),
    talla VARCHAR(20),
    descripcion VARCHAR(255),
    stock_actual INTEGER NOT NULL DEFAULT 0 CHECK (stock_actual >= 0),
    stock_minimo INTEGER NOT NULL DEFAULT 0 CHECK (stock_minimo >= 0),
    unidad_medida VARCHAR(20) NOT NULL DEFAULT 'unidad',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TRIGGER trg_articulos_updated_at
BEFORE UPDATE ON inventario.articulos
FOR EACH ROW EXECUTE FUNCTION inventario.set_updated_at();

CREATE INDEX IF NOT EXISTS idx_articulos_tipo ON inventario.articulos(tipo);

-- ------------------------------------------------------------
-- Movimientos de stock (entradas, salidas, ajustes)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventario.movimientos_stock (
    id_movimiento BIGSERIAL PRIMARY KEY,
    id_articulo BIGINT NOT NULL REFERENCES inventario.articulos(id_articulo),
    tipo_movimiento VARCHAR(10) NOT NULL CHECK (tipo_movimiento IN ('ENTRADA', 'SALIDA', 'AJUSTE')),
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    motivo VARCHAR(255),
    registrado_por_id_usuario BIGINT NOT NULL REFERENCES seguridad.usuarios(id_usuario),
    fecha_movimiento TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_movimientos_articulo
    ON inventario.movimientos_stock(id_articulo, fecha_movimiento DESC);

-- ------------------------------------------------------------
-- Asignaciones (entrega/devolucion a un estudiante o entrenador)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventario.asignaciones (
    id_asignacion BIGSERIAL PRIMARY KEY,
    id_articulo BIGINT NOT NULL REFERENCES inventario.articulos(id_articulo),
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    tipo_destinatario VARCHAR(15) NOT NULL CHECK (tipo_destinatario IN ('ESTUDIANTE', 'ENTRENADOR')),
    id_estudiante BIGINT REFERENCES academico.estudiantes(id_estudiante),
    id_entrenador BIGINT REFERENCES deportivo.entrenadores(id_entrenador),
    fecha_asignacion DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_devolucion_esperada DATE,
    fecha_devolucion_real DATE,
    estado VARCHAR(15) NOT NULL DEFAULT 'ASIGNADO' CHECK (estado IN ('ASIGNADO', 'DEVUELTO', 'PERDIDO')),
    registrado_por_id_usuario BIGINT NOT NULL REFERENCES seguridad.usuarios(id_usuario),
    observaciones VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_asignacion_destinatario CHECK (
        (tipo_destinatario = 'ESTUDIANTE' AND id_estudiante IS NOT NULL AND id_entrenador IS NULL)
        OR (tipo_destinatario = 'ENTRENADOR' AND id_entrenador IS NOT NULL AND id_estudiante IS NULL)
    )
);

CREATE TRIGGER trg_asignaciones_updated_at
BEFORE UPDATE ON inventario.asignaciones
FOR EACH ROW EXECUTE FUNCTION inventario.set_updated_at();

CREATE INDEX IF NOT EXISTS idx_asignaciones_estudiante
    ON inventario.asignaciones(id_estudiante) WHERE id_estudiante IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_asignaciones_entrenador
    ON inventario.asignaciones(id_entrenador) WHERE id_entrenador IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_asignaciones_estado
    ON inventario.asignaciones(estado);

-- ------------------------------------------------------------
-- Procedimiento almacenado: reporte de stock bajo (categoria
-- funcional "reporte" / calculo agregado, fuente en
-- db/procs/sp_reporte_stock_bajo.sql). Escalar OUT, igual que el resto
-- del catalogo: la lista de articulos bajo minimo se resuelve en el
-- repositorio JPA, el conteo agregado en el procedimiento.
-- ------------------------------------------------------------
CREATE OR REPLACE PROCEDURE inventario.sp_reporte_stock_bajo(
    OUT total_bajo_stock BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*)
      INTO total_bajo_stock
      FROM inventario.articulos
     WHERE activo = TRUE
       AND stock_actual <= stock_minimo;
END;
$$;
