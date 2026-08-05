package org.uteq.backend.academico.estudianteRepresentante.dto;

public record EstudianteRepresentanteResponse(
        Long idEstudianteRepresentante,
        Long idEstudiante,
        String codigoEstudiante,
        String nombreEstudiante,
        String apellidoEstudiante,
        Long idRepresentante,
        String nombreRepresentante,
        String apellidoRepresentante,
        String telefonoRepresentante,
        String relacion,
        Boolean contactoPrincipal
) {}
