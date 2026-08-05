package org.uteq.backend.seguridad.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Datos de presentacion de la sesion. Deliberadamente NO incluye el JWT: el
 * token viaja unicamente en la cookie HttpOnly (ADR-002, ADR-007). Un campo
 * de token aqui volveria legible por JavaScript exactamente lo que HttpOnly
 * existe para impedir, sin importar que la cookie tambien se emita.
 *
 * idPersona/idUsuario solo los rellena registro(): son IDs autoincrementales,
 * no secretos (EntrenadorResponse ya expone ambos hoy a cualquier
 * ADMINISTRADOR/ENTRENADOR), y le ahorran al frontend un round-trip para
 * encadenar la creacion de la fila de dominio (Entrenador/Representante)
 * justo despues de crear la cuenta. login()/me() no los usan.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SesionResponse {
    private String username;
    private String nombre;
    private String rol;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long idPersona;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long idUsuario;
}