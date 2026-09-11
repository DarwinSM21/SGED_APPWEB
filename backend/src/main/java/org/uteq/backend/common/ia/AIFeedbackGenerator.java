package org.uteq.backend.common.ia;

import java.util.List;

/**
 * Genera comentarios de apoyo (alineación, evaluación) a partir de un perfil
 * seudonimizado (RNF-16: nunca se envía al proveedor un dato que identifique
 * a un menor). Las implementaciones deben degradar de forma segura —{@link
 * FeedbackResult#unavailable(String)}, nunca una excepción— si el proveedor
 * falla o está deshabilitado.
 */
public interface AIFeedbackGenerator {

    /**
     * @param profile perfil seudonimizado del jugador
     * @return el comentario generado, o un resultado no disponible si el proveedor falla
     */
    FeedbackResult generatePlayerComment(AnonymousPlayerProfile profile);

    /**
     * @param lineup perfiles seudonimizados de los jugadores de la alineación
     * @return el comentario generado, o un resultado no disponible si el proveedor falla
     */
    FeedbackResult generateLineupComment(List<AnonymousPlayerProfile> lineup);

    /**
     * @return {@code true} si el proveedor está habilitado y configurado
     */
    boolean isAvailable();

    /**
     * Resultado de una generación de comentario: el texto, o —si no hubo
     * texto— el motivo por el que no lo hay.
     *
     * @param text texto generado, o {@code null} si no se pudo generar
     * @param reason motivo de la no disponibilidad, o {@code null} si {@code text} está presente
     */
    record FeedbackResult(String text, String reason) {

        /**
         * @param text texto generado
         * @return un resultado disponible con ese texto
         */
        public static FeedbackResult ok(String text) { return new FeedbackResult(text, null); }

        /**
         * @param reason motivo por el que no hay texto disponible
         * @return un resultado no disponible con ese motivo
         */
        public static FeedbackResult unavailable(String reason) { return new FeedbackResult(null, reason); }

        /**
         * @return {@code true} si este resultado trae un texto generado
         */
        public boolean isAvailable() { return text != null && !text.isBlank(); }
    }
}
