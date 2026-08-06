package org.uteq.backend.common.ia;

import java.util.Map;

/**
 * Datos de un jugador que SI pueden salir del sistema hacia un modelo de
 * lenguaje externo.
 *
 * <p>Este record existe para que la restriccion de privacidad no dependa de
 * que alguien se acuerde de aplicarla: el generador de feedback no recibe
 * entidades del dominio, recibe esto. No hay forma de pasarle un nombre, una
 * cedula, un correo o una fecha de nacimiento porque esos campos no existen
 * aqui. Si en el futuro alguien necesita enviarlos, tendra que modificar este
 * archivo, y ese cambio es visible en revision de codigo.
 *
 * <p>El motivo es concreto. El nivel gratuito de la API de Gemini permite que
 * el proveedor use los datos enviados para mejorar sus propios productos, y
 * los titulares de estos datos son menores de edad cuyo representante legal
 * todavia no tiene un mecanismo de consentimiento modelado en el sistema
 * (hallazgo H-04 de docs/etica/ETHICS.md). Enviar rendimiento deportivo
 * seudonimizado es defendible; enviar identidades no lo es.
 *
 * @param referencia    identificador opaco dentro de la peticion ("Jugador 1"),
 *                      sin relacion con la clave primaria ni con el codigo de
 *                      estudiante
 * @param categoria     nombre de la categoria del dia (por ejemplo "SUB-12")
 * @param posicion      posicion en la que jugo, o null
 * @param puntajes      puntaje por criterio ("Tecnica" -> 7.5)
 * @param puntajesPrevios promedio historico por criterio, para que el modelo
 *                      pueda hablar de evolucion y no solo del dia
 * @param asistenciasUltimoMes cuantas sesiones asistio en el ultimo mes
 * @param lesionado     si arrastra una lesion activa
 */
public record PerfilJugadorAnonimo(
        String referencia,
        String categoria,
        String posicion,
        Map<String, Double> puntajes,
        Map<String, Double> puntajesPrevios,
        Integer asistenciasUltimoMes,
        boolean lesionado
) {
    public PerfilJugadorAnonimo {
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("La referencia anonima es obligatoria");
        }
        puntajes = puntajes == null ? Map.of() : Map.copyOf(puntajes);
        puntajesPrevios = puntajesPrevios == null ? Map.of() : Map.copyOf(puntajesPrevios);
    }
}
