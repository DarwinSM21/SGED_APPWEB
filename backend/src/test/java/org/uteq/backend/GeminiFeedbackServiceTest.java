package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uteq.backend.common.ia.GeminiFeedbackService;
import org.uteq.backend.common.ia.PerfilJugadorAnonimo;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeminiFeedbackServiceTest {
    private static final PerfilJugadorAnonimo PERFIL = new PerfilJugadorAnonimo(
            "Jugador 1", "SUB-12", "Mediocentro",
            Map.of("Tecnica", 7.5, "Actitud", 9.0),
            Map.of("Tecnica", 6.8),
            12, false);

    private GeminiFeedbackService servicio(String apiKey, boolean habilitado) {
        return new GeminiFeedbackService(apiKey, "gemini-2.0-flash", habilitado, 8, 0);
    }

    @Test
    @DisplayName("Sin clave configurada el servicio queda no disponible, no falla")
    void sinClaveNoEstaDisponible() {
        var s = servicio("", true);
        assertFalse(s.estaDisponible());
    }

    @Test
    @DisplayName("Con clave pero deshabilitado explicitamente, tampoco esta disponible")
    void deshabilitadoNoEstaDisponible() {
        var s = servicio("clave-de-prueba", false);
        assertFalse(s.estaDisponible());
    }

    @Test
    @DisplayName("Pedir comentario sin proveedor devuelve motivo, nunca lanza excepcion")
    void comentarioJugadorSinProveedorNoLanza() {
        var s = servicio("", false);

        var r = assertDoesNotThrow(() -> s.generarComentarioJugador(PERFIL));

        assertFalse(r.disponible());
        assertNull(r.texto());
        assertNotNull(r.motivo(), "Debe explicar por que no hay texto");
    }

    @Test
    @DisplayName("Una alineacion vacia no se manda al modelo")
    void plantillaVaciaNoSeEnvia() {
        var s = servicio("clave-de-prueba", true);

        var r = s.generarComentarioPlantilla(List.of());

        assertFalse(r.disponible());
    }

    @Test
    @DisplayName("Un proveedor inalcanzable degrada a resultado no disponible, no rompe la evaluacion")
    void proveedorInalcanzableDegrada() {
        var s = servicio("clave-invalida-de-prueba", true);
        assertTrue(s.estaDisponible());

        var r = assertDoesNotThrow(() -> s.generarComentarioJugador(PERFIL));

        assertFalse(r.disponible());
        assertNotNull(r.motivo());
    }

    @Test
    @DisplayName("Con reintentos configurados sigue degradando limpio, sin lanzar ni colgarse")
    void conReintentosTambienDegrada() {
        var s = new GeminiFeedbackService("clave-invalida-de-prueba", "gemini-2.0-flash", true, 2, 2);

        var r = assertDoesNotThrow(() -> s.generarComentarioJugador(PERFIL));

        assertFalse(r.disponible());
        assertNull(r.texto());
        assertNotNull(r.motivo());
    }

    @Test
    @DisplayName("El perfil anonimo rechaza construirse sin referencia")
    void perfilExigeReferencia() {
        assertThrows(IllegalArgumentException.class,
                () -> new PerfilJugadorAnonimo(" ", "SUB-12", null, Map.of(), Map.of(), 0, false));
    }
}
