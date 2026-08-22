package org.uteq.backend.deportivo.categoria.dto;

import jakarta.validation.constraints.*;

public record CategoriaRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        // Formato fijo SUB-<edad>. Se acepta escrito de forma relajada
        // -"sub12", "Sub 12", "SUB-12"- y el servicio lo normaliza antes de
        // guardar: exigir la forma exacta al teclear solo cambia un error de
        // datos por un error de escritura, y lo que importa es que en la base
        // no queden tres categorias distintas que son la misma.
        @Pattern(regexp = "(?i)^\\s*sub[\\s-]?\\d{1,2}\\s*$",
                 message = "El nombre debe tener el formato SUB-12")
        String nombre,

        @NotNull(message = "La edad mínima es obligatoria")
        @Min(value = 4, message = "La edad mínima debe ser al menos 4 años")
        Short edadMin,

        @NotNull(message = "La edad máxima es obligatoria")
        Short edadMax,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String descripcion
) {}