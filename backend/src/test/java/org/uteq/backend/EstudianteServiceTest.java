package org.uteq.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.auth.entity.Persona;
import org.uteq.backend.auth.repository.PersonaRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.estudiante.dto.EstudianteRequest;
import org.uteq.backend.estudiante.dto.PageResponse;
import org.uteq.backend.estudiante.entity.Estudiante;
import org.uteq.backend.estudiante.repository.EstudianteRepository;
import org.uteq.backend.estudiante.service.EstudianteService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PersonaRepository personaRepository;
    @InjectMocks private EstudianteService service;

    private Estudiante estudiante() {
        Persona p = Persona.builder().nombre("Juan").apellido("Perez").activo(true).build();
        return Estudiante.builder().idEstudiante(1L).persona(p)
                .categoria("SUB-12").activo(true).build();
    }

    @Test
    void listar_devuelve_pagina_envuelta() {
        when(estudianteRepository.findByActivoTrue(any()))
                .thenReturn(new PageImpl<>(List.of(estudiante())));
        PageResponse<?> page = service.listar(PageRequest.of(0, 10));
        assertEquals(1, page.totalElements());
        assertEquals(1, page.content().size());
    }

    @Test
    void buscar_inexistente_lanza_404() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RecursoNoEncontradoException.class,
                () -> service.buscarPorId(99L));
    }

    @Test
    void crear_persiste_persona_y_estudiante() {
        when(personaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(estudianteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var resp = service.crear(new EstudianteRequest("Ana", "Vera", "SUB-10"));
        assertEquals("Ana", resp.nombre());
        assertEquals("SUB-10", resp.categoria());
    }

    @Test
    void eliminar_hace_baja_logica() {
        Estudiante e = estudiante();
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(e));
        when(estudianteRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        service.eliminar(1L);
        assertFalse(e.getActivo());
    }

    @Test
    void conteo_por_categoria_delega_en_funcion_sql() {
        when(estudianteRepository.contarActivosPorCategoria("SUB-12")).thenReturn(3L);
        assertEquals(3L, service.contarActivosPorCategoria("SUB-12"));
    }
}
