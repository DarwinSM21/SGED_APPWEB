package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uteq.backend.common.ia.OpenAiFeedbackService;
import org.uteq.backend.common.ia.PerfilJugadorAnonimo;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiFeedbackServiceTest {
    private static final PerfilJugadorAnonimo PERFIL = new PerfilJugadorAnonimo(
            "Jugador 1", "SUB-12", "Mediocentro",
            Map.of("Tecnica", 7.5, "Actitud", 9.0),
            Map.of("Tecnica", 6.8),
            12, false);

    private OpenAiFeedbackService servicio(String apiKey) {
        return new OpenAiFeedbackService(
                apiKey, "https://api.openai.com/v1", "gpt-4o-mini", 8, 0);
    }

    @Test
    @DisplayName("Sin clave configurada el servicio queda no disponible, no falla")
    void sinClaveNoEstaDisponible() {
        var s = servicio("");
        assertFalse(s.estaDisponible());
    }

    @Test
    @DisplayName("Una clave en blanco (solo espacios) tampoco habilita el servicio")
    void claveEnBlancoNoHabilita() {
        var s = servicio("   ");
        assertFalse(s.estaDisponible());
    }

    @Test
    @DisplayName("Con clave no vacia el servicio queda disponible")
    void conClaveEstaDisponible() {
        var s = servicio("clave-de-prueba");
        assertTrue(s.estaDisponible());
    }

    @Test
    @DisplayName("Pedir comentario sin clave devuelve motivo, nunca lanza excepcion")
    void comentarioJugadorSinClaveNoLanza() {
        var s = servicio("");

        var r = assertDoesNotThrow(() -> s.generarComentarioJugador(PERFIL));

        assertFalse(r.disponible());
        assertNull(r.texto());
        assertNotNull(r.motivo(), "Debe explicar por que no hay texto");
    }

    @Test
    @DisplayName("Una alineacion vacia no se manda al modelo")
    void plantillaVaciaNoSeEnvia() {
        var s = servicio("clave-de-prueba");

        var r = s.generarComentarioPlantilla(List.of());

        assertFalse(r.disponible());
    }

    @Test
    @DisplayName("Una alineacion nula tampoco se manda al modelo")
    void plantillaNulaNoSeEnvia() {
        var s = servicio("clave-de-prueba");

        var r = s.generarComentarioPlantilla(null);

        assertFalse(r.disponible());
    }

    @Test
    @DisplayName("Un proveedor inalcanzable o con clave invalida degrada, no rompe la evaluacion")
    void proveedorInalcanzableDegrada() {
        var s = servicio("clave-invalida-de-prueba");
        assertTrue(s.estaDisponible());

        var r = assertDoesNotThrow(() -> s.generarComentarioJugador(PERFIL));

        assertFalse(r.disponible());
        assertNotNull(r.motivo());
    }

    @Test
    @DisplayName("Con reintentos configurados sigue degradando limpio, sin lanzar ni colgarse")
    void conReintentosTambienDegrada() {
        var s = new OpenAiFeedbackService(
                "clave-invalida-de-prueba", "https://api.openai.com/v1", "gpt-4o-mini", 2, 2);

        var r = assertDoesNotThrow(() -> s.generarComentarioPlantilla(List.of(PERFIL)));

        assertFalse(r.disponible());
        assertNull(r.texto());
        assertNotNull(r.motivo());
    }

    @Test
    @DisplayName("Una URL base invalida tambien degrada limpio en vez de lanzar")
    void urlBaseInvalidaDegrada() {
        var s = new OpenAiFeedbackService(
                "clave-de-prueba", "http://host-que-no-existe.invalid", "gpt-4o-mini", 2, 0);

        var r = assertDoesNotThrow(() -> s.generarComentarioJugador(PERFIL));

        assertFalse(r.disponible());
        assertNotNull(r.motivo());
    }
}
