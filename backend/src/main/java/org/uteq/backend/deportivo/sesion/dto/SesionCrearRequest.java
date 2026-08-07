package org.uteq.backend.deportivo.sesion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * El entrenador nunca envia su propio idEntrenador aqui: se resuelve en el
 * controller desde el usuario autenticado, igual que en el resto del modulo
 * (ver SesionEntrenamientoController.hoy()). Evita que un entrenador cree una
 * sesion "a nombre" de otro con solo cambiar un id en el body.
 */
public record SesionCrearRequest(
        @NotNull Long idCategoria,
        @NotNull LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        String campo
) {}
