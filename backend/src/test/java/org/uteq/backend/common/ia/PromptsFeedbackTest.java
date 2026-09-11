package org.uteq.backend.common.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PromptsFeedbackTest {

    @Test
    @DisplayName("deJugador incluye la posicion cuando el perfil la trae")
    void deJugadorIncluyePosicionCuandoExiste() {
        var perfil = new AnonymousPlayerProfile("Jugador 1", "SUB-12", "Mediocentro",
                Map.of("Tecnica", 7.5), Map.of(), null, false);

        String prompt = PromptsFeedback.forPlayer(perfil);

        assertThat(prompt).contains("Posicion en la que juega: Mediocentro");
    }

    @Test
    @DisplayName("deJugador omite la linea de posicion cuando no viene en el perfil")
    void deJugadorOmitePosicionCuandoEsNula() {
        var perfil = new AnonymousPlayerProfile("Jugador 1", "SUB-12", null,
                Map.of("Tecnica", 7.5), Map.of(), null, false);

        String prompt = PromptsFeedback.forPlayer(perfil);

        assertThat(prompt).doesNotContain("Posicion en la que juega");
    }

    @Test
    @DisplayName("deJugador agrega el promedio historico solo si hay puntajes previos")
    void deJugadorAgregaHistoricoSoloSiExiste() {
        var conHistorico = new AnonymousPlayerProfile("Jugador 1", "SUB-12", null,
                Map.of("Tecnica", 7.5), Map.of("Tecnica", 6.0), null, false);
        var sinHistorico = new AnonymousPlayerProfile("Jugador 2", "SUB-12", null,
                Map.of("Tecnica", 7.5), Map.of(), null, false);

        assertThat(PromptsFeedback.forPlayer(conHistorico)).contains("Promedio historico");
        assertThat(PromptsFeedback.forPlayer(sinHistorico)).doesNotContain("Promedio historico");
    }

    @Test
    @DisplayName("deJugador reporta asistencias del ultimo mes solo cuando el dato existe")
    void deJugadorReportaAsistenciasSoloSiExiste() {
        var conAsistencias = new AnonymousPlayerProfile("Jugador 1", "SUB-12", null,
                Map.of("Tecnica", 7.5), Map.of(), 10, false);
        var sinAsistencias = new AnonymousPlayerProfile("Jugador 2", "SUB-12", null,
                Map.of("Tecnica", 7.5), Map.of(), null, false);

        assertThat(PromptsFeedback.forPlayer(conAsistencias))
                .contains("Entrenamientos a los que asistio en el ultimo mes: 10");
        assertThat(PromptsFeedback.forPlayer(sinAsistencias))
                .doesNotContain("Entrenamientos a los que asistio");
    }

    @Test
    @DisplayName("deJugador advierte sobre lesion activa solo cuando el jugador esta lesionado")
    void deJugadorAdvierteLesionSoloSiAplica() {
        var lesionado = new AnonymousPlayerProfile("Jugador 1", "SUB-12", null,
                Map.of("Tecnica", 7.5), Map.of(), null, true);
        var sano = new AnonymousPlayerProfile("Jugador 2", "SUB-12", null,
                Map.of("Tecnica", 7.5), Map.of(), null, false);

        assertThat(PromptsFeedback.forPlayer(lesionado)).contains("Arrastra una lesion activa");
        assertThat(PromptsFeedback.forPlayer(sano)).doesNotContain("Arrastra una lesion activa");
    }

    @Test
    @DisplayName("dePlantilla marca 'sin posicion' cuando un jugador de la alineacion no la tiene")
    void dePlantillaMarcaSinPosicionCuandoFalta() {
        var conPosicion = new AnonymousPlayerProfile("Jugador 1", "SUB-12", "Defensa",
                Map.of("Tecnica", 8.0), Map.of(), null, false);
        var sinPosicion = new AnonymousPlayerProfile("Jugador 2", "SUB-12", null,
                Map.of("Tecnica", 6.0), Map.of(), null, false);

        String prompt = PromptsFeedback.forLineup(List.of(conPosicion, sinPosicion));

        assertThat(prompt).contains("(Defensa)");
        assertThat(prompt).contains("(sin posicion)");
    }

    @Test
    @DisplayName("los puntajes vacios se reportan explicitamente como 'sin datos'")
    void puntajesVaciosSeReportanComoSinDatos() {
        var perfil = new AnonymousPlayerProfile("Jugador 1", "SUB-12", null,
                Map.of(), Map.of(), null, false);

        String prompt = PromptsFeedback.forLineup(List.of(perfil));

        assertThat(prompt).contains("sin datos");
    }
}
