package org.uteq.backend.deportivo.asistencia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;

/**
 * Lista manual del entrenador.
 *
 * <p>Hasta ahora la unica forma de registrar asistencia era que el propio
 * estudiante escaneara el QR. Eso deja fuera el caso normal: el chico que
 * vino a entrenar pero no trajo el telefono -en las categorias menores, casi
 * todos-. Sin una via manual esa asistencia no existe, y como todo lo que se
 * calcula despues (alertas de ausentismo, informe al representante, plantilla
 * sugerida) lee la misma tabla, el estudiante aparece ausente aunque estuvo.
 */
public class PasarListaDtos {

    public record MarcaAsistencia(
            @NotNull(message = "El estudiante es obligatorio")
            Long idEstudiante,

            // Los cuatro valores que ya admitia el CHECK de la tabla desde el
            // primer dia; el codigo solo escribia dos.
            @NotNull(message = "El estado es obligatorio")
            @Pattern(regexp = "PRESENTE|TARDE|AUSENTE|JUSTIFICADO",
                     message = "Estado no válido: use PRESENTE, TARDE, AUSENTE o JUSTIFICADO")
            String estado,

            @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
            String observacion
    ) {}

    public record PasarListaRequest(
            @NotEmpty(message = "Debe enviar al menos un estudiante")
            @Valid
            List<MarcaAsistencia> marcas
    ) {}

    /** Una fila de la nomina tal como la ve el entrenador al abrir la sesion. */
    public record FilaNomina(
            Long idEstudiante,
            String nombreCompleto,
            String estado,
            String metodo,
            LocalTime horaEntrada,
            String observacion
    ) {}

    public record NominaResponse(
            Long idSesion,
            String categoria,
            java.time.LocalDate fecha,
            LocalTime horaInicio,
            boolean editable,
            String motivoNoEditable,
            List<FilaNomina> filas
    ) {}
}
