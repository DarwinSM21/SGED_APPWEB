package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.uteq.backend.academico.asistencia.dto.AsistenciaRequest;
import org.uteq.backend.academico.asistencia.dto.AsistenciaResponse;
import org.uteq.backend.academico.asistencia.entity.Asistencia;
import org.uteq.backend.academico.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.academico.asistencia.service.AsistenciaService;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenadorCategoria.entity.EntrenadorCategoria;
import org.uteq.backend.deportivo.estadoAsistencia.entity.EstadoAsistencia;
import org.uteq.backend.deportivo.estadoAsistencia.repository.EstadoAsistenciaRepository;
import org.uteq.backend.deportivo.sesionEntrenamiento.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesionEntrenamiento.repository.SesionEntrenamientoRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private SesionEntrenamientoRepository sesionEntrenamientoRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private EstadoAsistenciaRepository estadoAsistenciaRepository;

    @InjectMocks private AsistenciaService service;

    private SesionEntrenamiento sesionDummy;
    private Estudiante estudianteDummy;
    private EstadoAsistencia estadoDummy;
    private Asistencia asistenciaDummy;

    @BeforeEach
    void setUp() {
        Entrenador entrenadorDummy = Entrenador.builder().idEntrenador(1L).build();
        Categoria categoriaDummy = Categoria.builder().idCategoria(1L).nombre("SUB-12").build();
        EntrenadorCategoria entrenadorCategoriaDummy = EntrenadorCategoria.builder()
                .idEntrenadorCategoria(1L)
                .entrenador(entrenadorDummy)
                .categoria(categoriaDummy)
                .build();

        sesionDummy = SesionEntrenamiento.builder()
                .idSesionEntrenamiento(1L)
                .entrenadorCategoria(entrenadorCategoriaDummy)
                .titulo("Entrenamiento tecnico")
                .fecha(LocalDate.now())
                .horaInicio(LocalTime.of(16, 0))
                .horaFin(LocalTime.of(18, 0))
                .build();

        Persona personaEstudiante = Persona.builder().idPersona(1L).nombre("Juan").apellido("Perez").build();
        estudianteDummy = Estudiante.builder()
                .idEstudiante(1L)
                .persona(personaEstudiante)
                .codigoEstudiante("EST-001")
                .activo(true)
                .build();

        estadoDummy = EstadoAsistencia.builder().idEstadoAsistencia(1L).nombre("PRESENTE").build();

        asistenciaDummy = Asistencia.builder()
                .idAsistencia(1L)
                .sesionEntrenamiento(sesionDummy)
                .estudiante(estudianteDummy)
                .estadoAsistencia(estadoDummy)
                .fechaRegistro(LocalDate.now())
                .build();
    }

    private AsistenciaRequest requestValido() {
        return new AsistenciaRequest(1L, 1L, 1L, LocalDate.now());
    }

    @Test
    @DisplayName("listar devuelve pagina mapeada")
    void listar_devuelve_pagina_mapeada() {
        Page<Asistencia> pagina = new PageImpl<>(List.of(asistenciaDummy), PageRequest.of(0, 10), 1);
        when(asistenciaRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        Page<AsistenciaResponse> resultado = service.listar(PageRequest.of(0, 10));

        assertEquals(1, resultado.getTotalElements());
        assertEquals("PRESENTE", resultado.getContent().get(0).nombreEstadoAsistencia());
    }

    @Test
    @DisplayName("listarPorEstudiante devuelve las asistencias del estudiante")
    void listarPorEstudiante_devuelve_lista() {
        when(asistenciaRepository.findByEstudiante_IdEstudiante(1L)).thenReturn(List.of(asistenciaDummy));

        List<AsistenciaResponse> resultado = service.listarPorEstudiante(1L);

        assertEquals(1, resultado.size());
        assertEquals("EST-001", resultado.get(0).codigoEstudiante());
    }

    @Test
    @DisplayName("listarPorSesion devuelve las asistencias de la sesion")
    void listarPorSesion_devuelve_lista() {
        when(asistenciaRepository.findBySesionEntrenamiento_IdSesionEntrenamiento(1L))
                .thenReturn(List.of(asistenciaDummy));

        List<AsistenciaResponse> resultado = service.listarPorSesion(1L);

        assertEquals(1, resultado.size());
        assertEquals("Entrenamiento tecnico", resultado.get(0).tituloSesion());
    }

    @Test
    @DisplayName("buscarPorId lanza excepcion cuando no existe")
    void buscarPorId_inexistente_lanza_excepcion() {
        when(asistenciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(99L));
    }

    @Test
    @DisplayName("crear persiste la asistencia cuando las referencias existen")
    void crear_persiste_asistencia_valida() {
        when(sesionEntrenamientoRepository.findById(1L)).thenReturn(Optional.of(sesionDummy));
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estadoAsistenciaRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(asistenciaRepository.save(any(Asistencia.class))).thenAnswer(i -> i.getArgument(0));

        AsistenciaResponse resultado = service.crear(requestValido());

        assertEquals("EST-001", resultado.codigoEstudiante());
        assertEquals("PRESENTE", resultado.nombreEstadoAsistencia());
    }

    @Test
    @DisplayName("crear usa la fecha actual cuando no se envia fechaRegistro")
    void crear_usa_fecha_actual_por_defecto() {
        when(sesionEntrenamientoRepository.findById(1L)).thenReturn(Optional.of(sesionDummy));
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estadoAsistenciaRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(asistenciaRepository.save(any(Asistencia.class))).thenAnswer(i -> i.getArgument(0));

        AsistenciaResponse resultado = service.crear(new AsistenciaRequest(1L, 1L, 1L, null));

        assertEquals(LocalDate.now(), resultado.fechaRegistro());
    }

    @Test
    @DisplayName("crear lanza excepcion si la sesion de entrenamiento no existe")
    void crear_sesion_inexistente_lanza_excepcion() {
        when(sesionEntrenamientoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.crear(requestValido()));
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar actualiza el estado de asistencia")
    void editar_actualiza_estado() {
        EstadoAsistencia nuevoEstado = EstadoAsistencia.builder().idEstadoAsistencia(2L).nombre("TARDE").build();

        when(asistenciaRepository.findById(1L)).thenReturn(Optional.of(asistenciaDummy));
        when(estadoAsistenciaRepository.findById(2L)).thenReturn(Optional.of(nuevoEstado));
        when(asistenciaRepository.save(any(Asistencia.class))).thenAnswer(i -> i.getArgument(0));

        AsistenciaResponse resultado = service.editar(1L, new AsistenciaRequest(1L, 1L, 2L, LocalDate.now()));

        assertEquals("TARDE", resultado.nombreEstadoAsistencia());
    }

    @Test
    @DisplayName("eliminar borra fisicamente el registro (sin columna activo)")
    void eliminar_hace_borrado_fisico() {
        when(asistenciaRepository.existsById(1L)).thenReturn(true);

        service.eliminar(1L);

        verify(asistenciaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("eliminar lanza excepcion cuando no existe")
    void eliminar_inexistente_lanza_excepcion() {
        when(asistenciaRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar(99L));
        verify(asistenciaRepository, never()).deleteById(any());
    }
}
