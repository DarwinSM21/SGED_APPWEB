package org.uteq.backend.academico.representante.dto;

public record RepresentanteResponse(
        Long idRepresentante,
        Long idPersona,
        String nombrePersona,
        String apellidoPersona,
        String cedula,
        String telefono,
        String ocupacion,
        Boolean activo
) {}
