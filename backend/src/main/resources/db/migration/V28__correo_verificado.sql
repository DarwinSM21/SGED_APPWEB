-- Flyway V28 — RNF-26 / hallazgo H-09 de docs/etica/ETHICS.md
--
-- Doble opt-in del correo de contacto. Hasta aqui el correo de una persona lo
-- registraba el ADMINISTRADOR al crear la cuenta y el sistema nunca comprobaba
-- que la direccion fuera real ni que perteneciera al titular; aun asi RF-37
-- enviaba el enlace de restablecimiento a ese valor.
--
-- A partir de esta migracion:
--   * seguridad.personas.correo_verificado marca si la direccion se confirmo;
--   * el alta o el cambio de correo la deja en FALSE y dispara un token de
--     confirmacion de un solo uso (EmailVerificationService);
--   * POST /api/auth/forgot no emite el enlace si el correo no esta verificado.
--
-- Las personas que ya existen se dan por verificadas (grandfathering): sus
-- cuentas las creo un administrador y cambiar su estado retroactivamente
-- romperia el flujo de recuperacion para usuarios legitimos. El requisito
-- aplica a las altas y cambios posteriores.

ALTER TABLE seguridad.personas
    ADD COLUMN IF NOT EXISTS correo_verificado BOOLEAN NOT NULL DEFAULT FALSE;

-- Grandfathering de las filas preexistentes.
UPDATE seguridad.personas SET correo_verificado = TRUE WHERE correo_verificado = FALSE;

-- Las altas nuevas nacen sin verificar.
ALTER TABLE seguridad.personas ALTER COLUMN correo_verificado SET DEFAULT FALSE;
