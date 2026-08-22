-- V20: permitir anular un pago mal registrado.
--
-- Hasta ahora un pago era inmutable: solo se podia crear y consultar. Eso
-- deja sin salida un error de digitacion -cobrar 2,50 donde eran 25,00- y
-- obliga a corregirlo tocando la base a mano.
--
-- Se anula, no se edita ni se borra. Editar el monto sobre el mismo registro
-- pisaria el dato anterior sin dejar rastro, que es justo lo contrario de lo
-- que persigue el modulo de auditoria; y borrar la fila haria desaparecer de
-- los totales un cobro que si ocurrio. Con la anulacion los dos registros
-- quedan visibles: el equivocado marcado como anulado y el correcto aparte.
--
-- Las tres columnas son NULL: un pago vigente simplemente no las tiene.

ALTER TABLE academico.pagos
    ADD COLUMN IF NOT EXISTS anulado_en TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS anulado_por_id_usuario BIGINT
        REFERENCES seguridad.usuarios(id_usuario),
    ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(255);

-- Anular exige decir por que: un registro anulado sin motivo no explica nada
-- a quien revise las cuentas despues.
ALTER TABLE academico.pagos
    DROP CONSTRAINT IF EXISTS chk_pago_anulacion_completa;
ALTER TABLE academico.pagos
    ADD CONSTRAINT chk_pago_anulacion_completa CHECK (
        (anulado_en IS NULL AND anulado_por_id_usuario IS NULL AND motivo_anulacion IS NULL)
        OR (anulado_en IS NOT NULL AND anulado_por_id_usuario IS NOT NULL
            AND motivo_anulacion IS NOT NULL)
    );

-- Los listados y los totales filtran por vigencia, asi que el indice va sobre
-- las filas que quedan activas.
CREATE INDEX IF NOT EXISTS idx_pagos_vigentes_por_fecha
    ON academico.pagos(fecha_pago)
    WHERE anulado_en IS NULL;

-- El indice que impide cobrar dos veces el mismo mes tiene que mirar solo los
-- pagos VIGENTES. Tal como estaba, anular una membresia dejaba el mes
-- bloqueado para siempre: el registro anulado seguia ocupando el hueco y el
-- cobro correcto se rechazaba con violacion de clave unica. Es el mismo patron
-- de indice parcial que ya usan idx_lesion_activa_por_estudiante e
-- idx_consentimiento_vigente.
DROP INDEX IF EXISTS academico.idx_pago_membresia_unico;
CREATE UNIQUE INDEX idx_pago_membresia_unico
    ON academico.pagos(id_estudiante, anio, mes)
    WHERE tipo = 'MEMBRESIA' AND anulado_en IS NULL;
