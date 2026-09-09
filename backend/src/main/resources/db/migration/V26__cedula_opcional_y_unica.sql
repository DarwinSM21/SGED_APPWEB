-- Flyway V26 — RF-49 / hallazgo H-01 de docs/etica/ETHICS.md
--
-- La cedula pasa a ser un dato OPCIONAL con unicidad garantizada cuando hay
-- valor. Hasta aqui: seguridad.personas.cedula era VARCHAR(10) sin NOT NULL
-- efectivo pero sin restriccion de unicidad; la entidad Person la declaraba
-- unique/not-null pero con ddl-auto=validate esas anotaciones nunca llegaron
-- al motor. El dígito verificador lo valida @Cedula en la capa de aplicacion.

-- 1. Opcional: garantiza que la columna admita NULL (no-op si ya lo admite).
ALTER TABLE seguridad.personas ALTER COLUMN cedula DROP NOT NULL;

-- 2. Unicidad parcial: dos personas no pueden compartir cedula, pero varias
--    pueden no tenerla. Un indice unico total serviria en PostgreSQL (permite
--    multiples NULL) pero el parcial deja explicita la intencion.
DROP INDEX IF EXISTS seguridad.idx_personas_cedula;
CREATE UNIQUE INDEX IF NOT EXISTS ux_personas_cedula_no_nula
    ON seguridad.personas (cedula)
    WHERE cedula IS NOT NULL;
