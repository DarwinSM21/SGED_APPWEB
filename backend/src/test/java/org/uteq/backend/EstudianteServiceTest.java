package org.uteq.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.uteq.backend.academico.estudiante.dto.EstudiantePageResponse;
import org.uteq.backend.academico.estudiante.dto.EstudianteRequest;
import org.uteq.backend.academico.estudiante.dto.EstudianteResponse;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.estudiante.service.EstudianteService;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private EstadoGeneralRepository estadoGeneralRepository;

    @InjectMocks private EstudianteService service;

    private Persona personaDummy() {
        return Persona.builder()
                .idPersona(1L)
                .nombre("Ana")
                .apellido("Gomez")
                .activo(true)
                .build();
    }

    private Categoria categoriaDummy() {
        return Categoria.builder()
                .idCategoria(1L)
                .nombre("SUB-12")
                .edadMin((short) 10)
                .edadMax((short) 12)
                .build();
    }

    private EstadoGeneral estadoDummy() {
        return EstadoGeneral.builder()
                .idEstadoGeneral(1L)
                .nombre("ACTIVO")
                .build();
    }

    private Estudiante estudiante() {
        return Estudiante.builder()
                .idEstudiante(1L)
                .persona(personaDummy())
                .categoria(categoriaDummy())
                .estadoGeneral(estadoDummy())
                .codigoEstudiante("EST-001")
                .fechaIngreso(LocalDate.now())
                .activo(true)
                .build();
    }

    @Test
    void listar_devuelve_pagina_envuelta() {
        when(estudianteRepository.findByActivoTrue(any()))
                .thenReturn(new PageImpl<>(List.of(estudiante())));

        EstudiantePageResponse<EstudianteResponse> page = service.listar(PageRequest.of(0, 10));

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
    void crear_asocia_persona_y_persiste_estudiante() {
        // Mocks para buscar entidades existentes
        when(personaRepository.findById(1L)).thenReturn(Optional.of(personaDummy()));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaDummy()));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy()));
        
        // Mock para guardar estudiante
        when(estudianteRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // DTO alineado a la nueva firma: (idPersona, idCategoria, idEstadoGeneral, codigo, fecha, peso, altura)
        EstudianteRequest request = new EstudianteRequest(
                1L, // idPersona
                1L, // idCategoria
                1L, // idEstadoGeneral
                "EST-002",
                LocalDate.now(),
                new BigDecimal("45.50"),
                new BigDecimal("1.50")
        );

        var resp = service.crear(request);

        assertNotNull(resp);
        assertEquals("Ana", resp.nombrePersona()); // Asegúrate de que este sea el nombre del campo en EstudianteResponse
        assertEquals("SUB-12", resp.nombreCategoria());
        assertEquals("EST-002", resp.codigoEstudiante());
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
        when(estudianteRepository.countByCategoria_IdCategoriaAndActivoTrue(1L)).thenReturn(3L);

        assertEquals(3L, service.contarActivosPorCategoria(1L));
    }
}