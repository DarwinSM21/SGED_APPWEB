package org.uteq.backend.common.ia;

import java.util.List;

public interface GeneradorFeedbackIA {
    ResultadoFeedback generarComentarioJugador(PerfilJugadorAnonimo perfil);

    ResultadoFeedback generarComentarioPlantilla(List<PerfilJugadorAnonimo> alineacion);

    boolean estaDisponible();

    record ResultadoFeedback(String texto, String motivo) {
        public static ResultadoFeedback ok(String texto) {
            return new ResultadoFeedback(texto, null);
        }

        public static ResultadoFeedback noDisponible(String motivo) {
            return new ResultadoFeedback(null, motivo);
        }

        public boolean disponible() {
            return texto != null && !texto.isBlank();
        }
    }
}
