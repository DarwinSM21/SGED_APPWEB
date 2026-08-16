package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.uteq.backend.academico.estudiante.dto.EstudiantePageResponse;
import org.uteq.backend.academico.estudiante.dto.EstudianteRequest;
import org.uteq.backend.academico.estudiante.dto.EstudianteResponse;
import org.uteq.backend.academico.estudiante.dto.HabilitarAccesoRequest;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.estudiante.service.EstudianteAccesoService;
import org.uteq.backend.academico.estudiante.service.EstudianteService;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.seguridad.estado.entity.EstadoGeneral;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.seguridad.persona.repository.PersonaRepository;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    @Mock private EstudianteRepository estudianteRepository;
    @Mock private PersonaRepository personaRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private EstadoGeneralRepository estadoGeneralRepository;
    @Mock private RepresentanteEstudianteRepository representanteEstudianteRepository;
    @Mock private EstudianteAccesoService estudianteAccesoService;

    @InjectMocks private EstudianteService service;

    private Persona personaDummy;
    private Categoria categoriaDummy;
    private EstadoGeneral estadoDummy;
    private Estudiante estudianteDummy;

    @BeforeEach
    void setUp() {
        personaDummy = Persona.builder()
                .idPersona(1L)
                .nombre("Ana")
                .apellido("Gomez")
                .activo(true)
                .build();

        categoriaDummy = Categoria.builder()
                .idCategoria(1L)
                .nombre("SUB-12")
                .edadMin((short) 10)
                .edadMax((short) 12)
                .build();

        estadoDummy = EstadoGeneral.builder()
                .idEstadoGeneral(1L)
                .nombre("ACTIVO")
                .build();

        estudianteDummy = Estudiante.builder()
                .idEstudiante(1L)
                .persona(personaDummy)
                .categoria(categoriaDummy)
                .estadoGeneral(estadoDummy)
                .codigoEstudiante("EST-001")
                .fechaIngreso(LocalDate.now())
                .peso(new BigDecimal("45.50"))
                .altura(new BigDecimal("1.50"))
                .activo(true)
                .createdAt(Instant.now())
                .build();
    }

    private EstudianteRequest crearRequestValido() {
        return new EstudianteRequest(
                1L,
                1L,
                1L,
                "EST-001",
                LocalDate.now(),
                new BigDecimal("45.50"),
                new BigDecimal("1.50")
        );
    }

    // --- PRUEBAS DE LISTADO Y BÚSQUEDA ---

    @Test
    @DisplayName("listar - Devuelve página envuelta de estudiantes activos")
    void listar_devuelve_pagina_envuelta() {
        when(estudianteRepository.findByActivoTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(estudianteDummy)));

        EstudiantePageResponse<EstudianteResponse> page = service.listar(PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.totalElements());
        assertEquals(1, page.content().size());
        assertEquals("Ana", page.content().get(0).nombrePersona());
    }

    @Test
    @DisplayName("buscarPorId - Devuelve el estudiante cuando existe y está activo")
    void buscarPorId_existente() {
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(1L)).thenReturn(Optional.of(estudianteDummy));

        EstudianteResponse resp = service.buscarPorId(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.idEstudiante());
        assertEquals("EST-001", resp.codigoEstudiante());
    }

    @Test
    @DisplayName("buscarPorId - Lanza RecursoNoEncontradoException cuando no existe")
    void buscarPorId_inexistente_lanza_404() {
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.buscarPorId(99L));
    }

    // --- PRUEBAS DE CREACIÓN ---

    @Test
    @DisplayName("crear - Persiste un nuevo estudiante correctamente cuando no existía previo")
    void crear_nuevo_estudiante_exito() {
        EstudianteRequest request = crearRequestValido();

        when(estudianteRepository.findByPersona_IdPersona(1L)).thenReturn(Optional.empty());
        when(estudianteRepository.existsByCodigoEstudiante("EST-001")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(personaDummy));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaDummy));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteResponse resp = service.crear(request);

        assertNotNull(resp);
        assertEquals("Ana", resp.nombrePersona());
        assertEquals("SUB-12", resp.nombreCategoria());
        assertEquals("EST-001", resp.codigoEstudiante());
        assertTrue(resp.activo());
    }

    @Test
    @DisplayName("crear - Lanza IllegalArgumentException si la persona ya tiene una ficha activa")
    void crear_persona_con_ficha_activa_lanza_excepcion() {
        EstudianteRequest request = crearRequestValido();
        when(estudianteRepository.findByPersona_IdPersona(1L)).thenReturn(Optional.of(estudianteDummy));

        assertThrows(IllegalArgumentException.class, () -> service.crear(request));
    }

    @Test
    @DisplayName("crear - Lanza IllegalArgumentException si la persona ya tiene cuenta con otro rol")
    void crear_persona_con_cuenta_de_otro_rol_lanza_excepcion() {
        EstudianteRequest request = crearRequestValido();
        doThrow(new IllegalArgumentException("La persona tiene una cuenta con otro rol"))
                .when(estudianteAccesoService).validarCoherenciaConFichaEstudiante(1L);

        assertThrows(IllegalArgumentException.class, () -> service.crear(request));

        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear - Acepta a la persona cuya cuenta ya tiene rol ESTUDIANTE")
    void crear_persona_con_cuenta_de_estudiante_pasa() {
        // No se stubea estudianteAccesoService.validarCoherenciaConFichaEstudiante:
        // un mock de un metodo void no hace nada por defecto, que es
        // exactamente el resultado de "la cuenta ya es de rol ESTUDIANTE".
        EstudianteRequest request = crearRequestValido();
        when(estudianteRepository.findByPersona_IdPersona(1L)).thenReturn(Optional.empty());
        when(estudianteRepository.existsByCodigoEstudiante("EST-001")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(personaDummy));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaDummy));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.crear(request));
    }

    @Test
    @DisplayName("crear - Reactiva ficha de estudiante si la persona tenía un registro inactivo")
    void crear_reactiva_estudiante_inactivo() {
        Estudiante estudianteInactivo = Estudiante.builder()
                .idEstudiante(1L)
                .persona(personaDummy)
                .activo(false)
                .build();

        EstudianteRequest request = crearRequestValido();

        when(estudianteRepository.findByPersona_IdPersona(1L)).thenReturn(Optional.of(estudianteInactivo));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaDummy));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteResponse resp = service.crear(request);

        assertNotNull(resp);
        assertTrue(estudianteInactivo.getActivo()); // Se verifica la reactivación
    }

    // --- PRUEBAS DE EDICIÓN ---

    @Test
    @DisplayName("editar - Actualiza los datos correctamente")
    void editar_estudiante_exito() {
        EstudianteRequest request = crearRequestValido();

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estudianteRepository.existsByCodigoEstudianteAndIdEstudianteNot("EST-001", 1L)).thenReturn(false);
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteResponse resp = service.editar(1L, request);

        assertNotNull(resp);
        assertEquals("EST-001", resp.codigoEstudiante());
        verify(estudianteRepository).save(any(Estudiante.class));
    }

    @Test
    @DisplayName("editar - Lanza excepción si el código de estudiante ya le pertenece a otro")
    void editar_codigo_duplicado_lanza_excepcion() {
        EstudianteRequest request = crearRequestValido();

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estudianteRepository.existsByCodigoEstudianteAndIdEstudianteNot("EST-001", 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.editar(1L, request));
    }

    // --- PRUEBAS DE ELIMINACIÓN Y CONTEO ---

    @Test
    @DisplayName("eliminar - Marca al estudiante como inactivo (Baja Lógica)")
    void eliminar_hace_baja_logica() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        service.eliminar(1L);

        assertFalse(estudianteDummy.getActivo());
        verify(estudianteRepository).save(estudianteDummy);
    }

    @Test
    @DisplayName("contarActivosPorCategoria - Delega al SP via @Procedure")
    void conteo_por_categoria_delega_en_repositorio() {
        when(estudianteRepository.contarEstudiantesActivosPorCategoria(1L)).thenReturn(3L);

        long conteo = service.contarActivosPorCategoria(1L);

        assertEquals(3L, conteo);
    }

    @Test
    @DisplayName("desactivarPorCategoria - Delega al SP via @Procedure")
    void desactivarCategoria_delega_en_sp() {
        doNothing().when(estudianteRepository).desactivarEstudiantesPorCategoria(1L);
        service.desactivarPorCategoria(1L);
        verify(estudianteRepository).desactivarEstudiantesPorCategoria(1L);
    }

    @Test
    @DisplayName("generarSiguienteCodigo - Delega al SP via @Procedure")
    void generarSiguienteCodigo_delega_en_sp() {
        when(estudianteRepository.generarSiguienteCodigo(2026)).thenReturn("EST-2026-0007");

        String codigo = service.generarSiguienteCodigo(2026);

        assertEquals("EST-2026-0007", codigo);
    }

    @Test
    @DisplayName("contactoDeEmergencia - Delega al SP via @Procedure cuando el estudiante existe")
    void contactoDeEmergencia_delega_en_sp() {
        when(estudianteRepository.existsById(1L)).thenReturn(true);
        when(representanteEstudianteRepository.contactoDe(1L)).thenReturn("Maria Perez - 0991234567");

        String contacto = service.contactoDeEmergencia(1L);

        assertEquals("Maria Perez - 0991234567", contacto);
    }

    @Test
    @DisplayName("contactoDeEmergencia - Lanza RecursoNoEncontradoException si el estudiante no existe")
    void contactoDeEmergencia_estudiante_inexistente_lanza_404() {
        when(estudianteRepository.existsById(99L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> service.contactoDeEmergencia(99L));
        verify(representanteEstudianteRepository, never()).contactoDe(any());
    }

    // --- PRUEBAS DE HABILITAR ACCESO (rol ESTUDIANTE) ---

    @Test
    @DisplayName("habilitarAcceso - Crea el usuario sobre la Persona YA existente, no una nueva")
    void habilitarAcceso_crea_usuario_sobre_persona_existente() {
        HabilitarAccesoRequest request = new HabilitarAccesoRequest("andres@sged.test", "password123");
        Usuario usuarioCreado = Usuario.builder().idUsuario(9L).persona(personaDummy).build();

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estudianteAccesoService.crearCuentaDeEstudiante(personaDummy, request)).thenReturn(usuarioCreado);
        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(i -> i.getArgument(0));

        EstudianteResponse resp = service.habilitarAcceso(1L, request);

        assertNotNull(resp);
        verify(estudianteAccesoService).crearCuentaDeEstudiante(personaDummy, request);
        verify(personaRepository, never()).save(any());
        assertSame(usuarioCreado, estudianteDummy.getUsuario());
    }

    @Test
    @DisplayName("habilitarAcceso - Rechaza si el estudiante ya tiene una cuenta")
    void habilitarAcceso_rechaza_si_ya_tiene_cuenta() {
        estudianteDummy.setUsuario(Usuario.builder().idUsuario(5L).build());
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));

        assertThrows(IllegalArgumentException.class,
                () -> service.habilitarAcceso(1L, new HabilitarAccesoRequest("x@sged.test", "password123")));

        verify(estudianteAccesoService, never()).crearCuentaDeEstudiante(any(), any());
    }

    @Test
    @DisplayName("habilitarAcceso - Propaga el rechazo de EstudianteAccesoService si el username ya esta en uso")
    void habilitarAcceso_rechaza_username_duplicado() {
        HabilitarAccesoRequest request = new HabilitarAccesoRequest("dup@sged.test", "password123");
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        doThrow(new IllegalArgumentException("Ya existe una cuenta con ese usuario"))
                .when(estudianteAccesoService).crearCuentaDeEstudiante(personaDummy, request);

        assertThrows(IllegalArgumentException.class, () -> service.habilitarAcceso(1L, request));

        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("habilitarAcceso - Lanza RecursoNoEncontradoException si el estudiante no existe")
    void habilitarAcceso_estudiante_inexistente_lanza_404() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.habilitarAcceso(99L, new HabilitarAccesoRequest("x@sged.test", "password123")));
    }
}