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
import org.uteq.backend.academico.student.dto.StudentPageResponse;
import org.uteq.backend.academico.student.dto.StudentRequest;
import org.uteq.backend.academico.student.dto.StudentResponse;
import org.uteq.backend.academico.student.dto.EnableAccessRequest;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.student.service.StudentAccessService;
import org.uteq.backend.academico.student.service.StudentService;
import org.uteq.backend.academico.guardian.repository.GuardianStudentRepository;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.person.repository.PersonRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock private StudentRepository estudianteRepository;
    @Mock private PersonRepository personaRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private GeneralStatusRepository estadoGeneralRepository;
    @Mock private GuardianStudentRepository representanteEstudianteRepository;
    @Mock private StudentAccessService estudianteAccesoService;

    @InjectMocks private StudentService service;

    private Person personaDummy;
    private Categoria categoriaDummy;
    private GeneralStatus estadoDummy;
    private Student estudianteDummy;

    @BeforeEach
    void setUp() {
        personaDummy = Person.builder()
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

        estadoDummy = GeneralStatus.builder()
                .idEstadoGeneral(1L)
                .nombre("ACTIVO")
                .build();

        estudianteDummy = Student.builder()
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

    private StudentRequest crearRequestValido() {
        return new StudentRequest(
                1L,
                1L,
                1L,
                "EST-001",
                LocalDate.now(),
                new BigDecimal("45.50"),
                new BigDecimal("1.50"),
                null
        );
    }

    @Test
    @DisplayName("listar - Devuelve página envuelta de estudiantes activos")
    void listar_devuelve_pagina_envuelta() {
        when(estudianteRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(estudianteDummy)));

        StudentPageResponse<StudentResponse> page = service.list(PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.totalElements());
        assertEquals(1, page.content().size());
        assertEquals("Ana", page.content().get(0).nombrePersona());
    }

    @Test
    @DisplayName("buscarPorId - Devuelve el estudiante cuando existe y está activo")
    void buscarPorId_existente() {
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(1L)).thenReturn(Optional.of(estudianteDummy));

        StudentResponse resp = service.findById(1L);

        assertNotNull(resp);
        assertEquals(1L, resp.idEstudiante());
        assertEquals("EST-001", resp.codigoEstudiante());
    }

    @Test
    @DisplayName("buscarPorId - Lanza ResourceNotFoundException cuando no existe")
    void buscarPorId_inexistente_lanza_404() {
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    @DisplayName("crear - Persiste un nuevo estudiante correctamente cuando no existía previo")
    void crear_nuevo_estudiante_exito() {
        StudentRequest request = crearRequestValido();

        when(estudianteRepository.findByPersona_IdPersona(1L)).thenReturn(Optional.empty());
        when(estudianteRepository.existsByCodigoEstudiante("EST-001")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(personaDummy));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaDummy));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(estudianteRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponse resp = service.create(request);

        assertNotNull(resp);
        assertEquals("Ana", resp.nombrePersona());
        assertEquals("SUB-12", resp.nombreCategoria());
        assertEquals("EST-001", resp.codigoEstudiante());
        assertTrue(resp.activo());
    }

    @Test
    @DisplayName("crear - Lanza IllegalArgumentException si la persona ya tiene una ficha activa")
    void crear_persona_con_ficha_activa_lanza_excepcion() {
        StudentRequest request = crearRequestValido();
        when(estudianteRepository.findByPersona_IdPersona(1L)).thenReturn(Optional.of(estudianteDummy));

        assertThrows(IllegalArgumentException.class, () -> service.create(request));
    }

    @Test
    @DisplayName("crear - Lanza IllegalArgumentException si la persona ya tiene cuenta con otro rol")
    void crear_persona_con_cuenta_de_otro_rol_lanza_excepcion() {
        StudentRequest request = crearRequestValido();
        doThrow(new IllegalArgumentException("La persona tiene una cuenta con otro rol"))
                .when(estudianteAccesoService).validateConsistencyWithStudentRecord(1L);

        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("crear - Acepta a la persona cuya cuenta ya tiene rol ESTUDIANTE")
    void crear_persona_con_cuenta_de_estudiante_pasa() {
        StudentRequest request = crearRequestValido();
        when(estudianteRepository.findByPersona_IdPersona(1L)).thenReturn(Optional.empty());
        when(estudianteRepository.existsByCodigoEstudiante("EST-001")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(personaDummy));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaDummy));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(estudianteRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.create(request));
    }

    @Test
    @DisplayName("crear - Reactiva ficha de estudiante si la persona tenía un registro inactivo")
    void crear_reactiva_estudiante_inactivo() {
        Student estudianteInactivo = Student.builder()
                .idEstudiante(1L)
                .persona(personaDummy)
                .activo(false)
                .build();

        StudentRequest request = crearRequestValido();

        when(estudianteRepository.findByPersona_IdPersona(1L)).thenReturn(Optional.of(estudianteInactivo));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaDummy));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(estudianteRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponse resp = service.create(request);

        assertNotNull(resp);
        assertTrue(estudianteInactivo.getActivo());
    }

    @Test
    @DisplayName("editar - Actualiza los datos correctamente")
    void editar_estudiante_exito() {
        StudentRequest request = crearRequestValido();

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estudianteRepository.existsByCodigoEstudianteAndIdEstudianteNot("EST-001", 1L)).thenReturn(false);
        when(estudianteRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponse resp = service.update(1L, request);

        assertNotNull(resp);
        assertEquals("EST-001", resp.codigoEstudiante());
        verify(estudianteRepository).save(any(Student.class));
    }

    @Test
    @DisplayName("editar - Lanza excepción si el código de estudiante ya le pertenece a otro")
    void editar_codigo_duplicado_lanza_excepcion() {
        StudentRequest request = crearRequestValido();

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estudianteRepository.existsByCodigoEstudianteAndIdEstudianteNot("EST-001", 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.update(1L, request));
    }

    @Test
    @DisplayName("eliminar - Marca al estudiante como inactivo (Baja Lógica)")
    void eliminar_hace_baja_logica() {
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estudianteRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        service.delete(1L);

        assertFalse(estudianteDummy.getActivo());
        verify(estudianteRepository).save(estudianteDummy);
    }

    @Test
    @DisplayName("contarActivosPorCategoria - Delega al SP via @Procedure")
    void conteo_por_categoria_delega_en_repositorio() {
        when(estudianteRepository.countActiveStudentsByCategory(1L)).thenReturn(3L);

        long conteo = service.countActiveByCategory(1L);

        assertEquals(3L, conteo);
    }

    @Test
    @DisplayName("desactivarPorCategoria - Delega al SP via @Procedure")
    void desactivarCategoria_delega_en_sp() {
        doNothing().when(estudianteRepository).deactivateStudentsByCategory(1L);
        service.deactivateByCategory(1L);
        verify(estudianteRepository).deactivateStudentsByCategory(1L);
    }

    @Test
    @DisplayName("generarSiguienteCodigo - Delega al SP via @Procedure")
    void generarSiguienteCodigo_delega_en_sp() {
        when(estudianteRepository.generateNextCode(2026)).thenReturn("EST-2026-0007");

        String codigo = service.generateNextCode(2026);

        assertEquals("EST-2026-0007", codigo);
    }

    @Test
    @DisplayName("contactoDeEmergencia - Delega al SP via @Procedure cuando el estudiante existe")
    void contactoDeEmergencia_delega_en_sp() {
        when(estudianteRepository.existsById(1L)).thenReturn(true);
        when(representanteEstudianteRepository.contactoDe(1L)).thenReturn("Maria Perez - 0991234567");

        String contacto = service.emergencyContact(1L);

        assertEquals("Maria Perez - 0991234567", contacto);
    }

    @Test
    @DisplayName("contactoDeEmergencia - Lanza ResourceNotFoundException si el estudiante no existe")
    void contactoDeEmergencia_estudiante_inexistente_lanza_404() {
        when(estudianteRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.emergencyContact(99L));
        verify(representanteEstudianteRepository, never()).contactoDe(any());
    }

    @Test
    @DisplayName("habilitarAcceso - Crea el usuario sobre la Persona YA existente, no una nueva")
    void habilitarAcceso_crea_usuario_sobre_persona_existente() {
        EnableAccessRequest request = new EnableAccessRequest("andres@sged.test", "password123");
        UserAccount usuarioCreado = UserAccount.builder().idUsuario(9L).persona(personaDummy).build();

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        when(estudianteAccesoService.createStudentAccount(personaDummy, request)).thenReturn(usuarioCreado);
        when(estudianteRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        StudentResponse resp = service.enableAccess(1L, request);

        assertNotNull(resp);
        verify(estudianteAccesoService).createStudentAccount(personaDummy, request);
        verify(personaRepository, never()).save(any());
        assertSame(usuarioCreado, estudianteDummy.getUsuario());
    }

    @Test
    @DisplayName("habilitarAcceso - Rechaza si el estudiante ya tiene una cuenta")
    void habilitarAcceso_rechaza_si_ya_tiene_cuenta() {
        estudianteDummy.setUsuario(UserAccount.builder().idUsuario(5L).build());
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));

        assertThrows(IllegalArgumentException.class,
                () -> service.enableAccess(1L, new EnableAccessRequest("x@sged.test", "password123")));

        verify(estudianteAccesoService, never()).createStudentAccount(any(), any());
    }

    @Test
    @DisplayName("habilitarAcceso - Propaga el rechazo de StudentAccessService si el username ya esta en uso")
    void habilitarAcceso_rechaza_username_duplicado() {
        EnableAccessRequest request = new EnableAccessRequest("dup@sged.test", "password123");
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(estudianteDummy));
        doThrow(new IllegalArgumentException("Ya existe una cuenta con ese usuario"))
                .when(estudianteAccesoService).createStudentAccount(personaDummy, request);

        assertThrows(IllegalArgumentException.class, () -> service.enableAccess(1L, request));

        verify(estudianteRepository, never()).save(any());
    }

    @Test
    @DisplayName("habilitarAcceso - Lanza ResourceNotFoundException si el estudiante no existe")
    void habilitarAcceso_estudiante_inexistente_lanza_404() {
        when(estudianteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.enableAccess(99L, new EnableAccessRequest("x@sged.test", "password123")));
    }

    private Person personaDeEdad(int anios) {
        return Person.builder()
                .idPersona(1L)
                .nombre("Ana")
                .apellido("Gomez")
                .fechaNacimiento(LocalDate.now().minusYears(anios).minusDays(1))
                .activo(true)
                .build();
    }

    private void prepararCrear(Person persona) {
        when(estudianteRepository.findByPersona_IdPersona(1L)).thenReturn(Optional.empty());
        when(estudianteRepository.existsByCodigoEstudiante("EST-001")).thenReturn(false);
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaDummy));
    }

    @Test
    void crear_con_edad_fuera_del_rango_de_la_categoria_lanza_excepcion() {
        prepararCrear(personaDeEdad(18));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.create(crearRequestValido()));

        assertTrue(e.getMessage().contains("18"), e.getMessage());
        assertTrue(e.getMessage().contains("SUB-12"), e.getMessage());
        verify(estudianteRepository, never()).save(any(Student.class));
    }

    @Test
    void crear_con_edad_dentro_del_rango_guarda_normalmente() {
        prepararCrear(personaDeEdad(11));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(estudianteRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.create(crearRequestValido()));
    }

    @Test
    void crear_en_el_borde_del_rango_es_valido() {
        prepararCrear(personaDeEdad(12));
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(estudianteRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.create(crearRequestValido()));
    }

    @Test
    void crear_sin_fecha_de_nacimiento_no_bloquea() {
        prepararCrear(personaDummy);
        when(estadoGeneralRepository.findById(1L)).thenReturn(Optional.of(estadoDummy));
        when(estudianteRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        assertNotNull(service.create(crearRequestValido()));
    }
}
