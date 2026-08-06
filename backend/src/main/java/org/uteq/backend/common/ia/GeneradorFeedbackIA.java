package org.uteq.backend.common.ia;

import java.util.List;

/**
 * Genera retroalimentacion en texto a partir de datos deportivos
 * seudonimizados.
 *
 * <p>Es una interfaz y no una clase concreta por dos razones. La primera es
 * que el proveedor de modelo es una decision reversible: hoy es Gemini en su
 * nivel gratuito por costo, y esa decision puede cambiar sin tocar el dominio.
 * La segunda es que las pruebas necesitan una implementacion que no salga a
 * internet.
 */
public interface GeneradorFeedbackIA {

    /**
     * Redacta un comentario breve sobre el desempeno de un jugador.
     *
     * <p>Nunca lanza excepcion por fallo del proveedor: la generacion de texto
     * es un complemento, no el trabajo del entrenador. Si el modelo no
     * responde, devuelve un resultado no disponible con el motivo, y quien
     * llama decide si lo muestra o lo omite. Perder la evaluacion cargada a
     * mano porque un servicio externo esta caido seria un defecto grave.
     */
    ResultadoFeedback generarComentarioJugador(PerfilJugadorAnonimo perfil);

    /**
     * Redacta un comentario sobre una alineacion sugerida.
     *
     * <p>Importante: la seleccion y el orden de los jugadores NO los decide el
     * modelo. Eso es logica deterministica (filtros y orden por puntaje) que
     * vive en el dominio y es auditable. El modelo solo pone en palabras una
     * decision ya tomada.
     */
    ResultadoFeedback generarComentarioPlantilla(List<PerfilJugadorAnonimo> alineacion);

    /** Indica si hay un proveedor configurado y habilitado. */
    boolean estaDisponible();

    /**
     * Resultado de una generacion.
     *
     * @param texto     el comentario, o null si no se pudo generar
     * @param motivo    por que no se pudo generar, o null si todo fue bien
     */
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
