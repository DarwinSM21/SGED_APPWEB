package org.uteq.backend.common;

import java.time.ZoneId;

/**
 * Zona horaria del negocio (Ecuador, UTC-5 sin horario de verano) -no la del
 * contenedor, que corre en UTC-. LocalDate.now() sin zona explicita usa la
 * del JVM: en un momento dado puede ser "manana" en UTC pero todavia "hoy"
 * en Ecuador, lo que desalinea cualquier comparacion de fecha hecha en el
 * servidor contra datos que el usuario genero pensando en su propia fecha.
 */
public final class Zonas {

    public static final ZoneId ECUADOR = ZoneId.of("America/Guayaquil");

    private Zonas() {}
}
