package org.uteq.backend.auth.dto;

import lombok.Builder;

/**
 * Respuesta de login/refresh desde la Entrega 3: el JWT ya NO viaja en el
 * body sino en cookies HttpOnly (Bloque A.1). El body solo lleva datos
 * de presentación para el frontend.
 */
@Builder
public record SesionResponse(String username, String nombre, String rol) {
}
