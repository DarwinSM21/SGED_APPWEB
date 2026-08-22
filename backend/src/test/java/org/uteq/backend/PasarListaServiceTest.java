package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;
import org.uteq.backend.academico.representante.service.NotificacionService;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.deportivo.asistencia.dto.PasarListaDtos.MarcaAsistencia;
import org.uteq.backend.deportivo.asistencia.dto.PasarListaDtos.PasarListaRequest;
import org.uteq.backend.deportivo.asistencia.entity.Asistencia;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.asistencia.service.AsistenciaService;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lista manual del entrenador.
 *
 * <p>Cubre la via que hasta ahora no existia: antes la unica forma de que un
 * estudiante quedara registrado como presente era que el mismo escaneara el
 * QR, de modo que un entrenamiento entero sin telefonos quedaba como
 * inasistencia general.
 */
@ExtendWith(MockitoExtension.class)
class PasarListaServiceTest {

    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private EstudianteRepository estudianteRepository;
    @Mock private SesionEntrenamientoRepository sesionRepository;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private AsistenciaService asistenciaService;

    private static final Long ID_CATEGORIA = 3L;
    private static final Long ID_SESION = 77L;

    private Categoria categoria(Long id, String nombre) {
        return Categoria.builder().idCategoria(id).nombre(nombre).build();
    }

    private Estudiante estudiante(Long id, Long idCategoria) {
        return Estudiante.builder()
                .idEstudiante(id)
                .persona(Persona.builder().nombre("Ana").apellido("Vera").build())
                .categoria(categoria(idCategoria, "SUB-18"))
                .build();
    }

    private SesionEntrenamiento sesion(LocalDate fecha) {
        return SesionEntrenamiento.builder()
                .idSesion(ID_SESION)
                .fecha(fecha)
                .horaInicio(LocalTime.of(18, 0))
                .categoria(categoria(ID_CATEGORIA, "SUB-18"))
                .build();
    }

    private PasarListaRequest lista(Long idEstudiante, String estado) {
        return new PasarListaRequest(List.of(new MarcaAsistencia(idEstudiante, estado, null)));
    }

    @Test
    @DisplayName("pasarLista registra al estudiante que no escaneo el QR")
    void pasarLista_registra_al_que_no_escaneo() {
        var hoy = LocalDate.now(Zonas.ECUADOR);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion(hoy)));
        when(asistenciaRepository.findBySesionIdSesion(ID_SESION)).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(6L))
                .thenReturn(Optional.of(estudiante(6L, ID_CATEGORIA)));
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(ID_CATEGORIA))
                .thenReturn(List.of(estudiante(6L, ID_CATEGORIA)));

        asistenciaService.pasarLista(ID_SESION, lista(6L, Asistencia.ESTADO_PRESENTE));

        ArgumentCaptor<Asistencia> capturada = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepository).save(capturada.capture());
        assertThat(capturada.getValue().getEstado()).isEqualTo(Asistencia.ESTADO_PRESENTE);
        assertThat(capturada.getValue().getMetodo()).isEqualTo(Asistencia.METODO_MANUAL);
    }

    @Test
    @DisplayName("la lista manual no inventa una hora de llegada")
    void pasarLista_no_inventa_hora_de_llegada() {
        var hoy = LocalDate.now(Zonas.ECUADOR);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion(hoy)));
        when(asistenciaRepository.findBySesionIdSesion(ID_SESION)).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(6L))
                .thenReturn(Optional.of(estudiante(6L, ID_CATEGORIA)));
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(ID_CATEGORIA))
                .thenReturn(List.of(estudiante(6L, ID_CATEGORIA)));

        asistenciaService.pasarLista(ID_SESION, lista(6L, Asistencia.ESTADO_PRESENTE));

        ArgumentCaptor<Asistencia> capturada = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepository).save(capturada.capture());
        // El entrenador afirma que estuvo, no a que hora entro. Escribir
        // LocalTime.now() aqui guardaria la hora en que se tecleo la lista.
        assertThat(capturada.getValue().getHoraEntrada()).isNull();
    }

    @Test
    @DisplayName("corregir a PRESENTE conserva la hora real que ya habia medido el QR")
    void pasarLista_conserva_la_hora_del_qr() {
        var hoy = LocalDate.now(Zonas.ECUADOR);
        var horaReal = LocalTime.of(18, 3, 12);
        var yaMarcada = Asistencia.builder()
                .estudiante(estudiante(6L, ID_CATEGORIA))
                .estado(Asistencia.ESTADO_PRESENTE)
                .metodo(Asistencia.METODO_QR)
                .horaEntrada(horaReal)
                .build();

        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion(hoy)));
        when(asistenciaRepository.findBySesionIdSesion(ID_SESION)).thenReturn(List.of(yaMarcada));
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(6L))
                .thenReturn(Optional.of(estudiante(6L, ID_CATEGORIA)));
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(ID_CATEGORIA))
                .thenReturn(List.of(estudiante(6L, ID_CATEGORIA)));

        asistenciaService.pasarLista(ID_SESION, lista(6L, Asistencia.ESTADO_TARDE));

        ArgumentCaptor<Asistencia> capturada = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepository).save(capturada.capture());
        assertThat(capturada.getValue().getEstado()).isEqualTo(Asistencia.ESTADO_TARDE);
        assertThat(capturada.getValue().getHoraEntrada()).isEqualTo(horaReal);
        assertThat(capturada.getValue().getMetodo()).isEqualTo(Asistencia.METODO_QR);
    }

    @Test
    @DisplayName("marcar AUSENTE borra la hora de entrada que hubiera")
    void pasarLista_ausente_borra_la_hora() {
        var hoy = LocalDate.now(Zonas.ECUADOR);
        var yaMarcada = Asistencia.builder()
                .estudiante(estudiante(6L, ID_CATEGORIA))
                .estado(Asistencia.ESTADO_PRESENTE)
                .metodo(Asistencia.METODO_QR)
                .horaEntrada(LocalTime.of(18, 3))
                .build();

        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion(hoy)));
        when(asistenciaRepository.findBySesionIdSesion(ID_SESION)).thenReturn(List.of(yaMarcada));
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(6L))
                .thenReturn(Optional.of(estudiante(6L, ID_CATEGORIA)));
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(ID_CATEGORIA))
                .thenReturn(List.of(estudiante(6L, ID_CATEGORIA)));

        asistenciaService.pasarLista(ID_SESION, lista(6L, Asistencia.ESTADO_AUSENTE));

        ArgumentCaptor<Asistencia> capturada = ArgumentCaptor.forClass(Asistencia.class);
        verify(asistenciaRepository).save(capturada.capture());
        assertThat(capturada.getValue().getHoraEntrada()).isNull();
    }

    @Test
    @DisplayName("no se puede pasar lista de una sesion que todavia no ocurrio")
    void pasarLista_rechaza_sesion_futura() {
        var manana = LocalDate.now(Zonas.ECUADOR).plusDays(1);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion(manana)));

        assertThatThrownBy(() -> asistenciaService.pasarLista(ID_SESION, lista(6L, Asistencia.ESTADO_PRESENTE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("todavía no ocurre");

        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("un estudiante de otra categoria no entra en la lista de esta sesion")
    void pasarLista_rechaza_estudiante_de_otra_categoria() {
        var hoy = LocalDate.now(Zonas.ECUADOR);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion(hoy)));
        when(asistenciaRepository.findBySesionIdSesion(ID_SESION)).thenReturn(List.of());
        when(estudianteRepository.findByIdEstudianteAndActivoTrue(9L))
                .thenReturn(Optional.of(estudiante(9L, 99L)));

        assertThatThrownBy(() -> asistenciaService.pasarLista(ID_SESION, lista(9L, Asistencia.ESTADO_PRESENTE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece a");

        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("la nomina lista a toda la categoria, no solo a quienes ya marcaron")
    void nomina_incluye_a_los_que_faltan_por_marcar() {
        var hoy = LocalDate.now(Zonas.ECUADOR);
        var conMarca = Asistencia.builder()
                .estudiante(estudiante(6L, ID_CATEGORIA))
                .estado(Asistencia.ESTADO_PRESENTE)
                .metodo(Asistencia.METODO_QR)
                .horaEntrada(LocalTime.of(18, 1))
                .build();

        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion(hoy)));
        when(asistenciaRepository.findBySesionIdSesion(ID_SESION)).thenReturn(List.of(conMarca));
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(ID_CATEGORIA))
                .thenReturn(List.of(estudiante(6L, ID_CATEGORIA), estudiante(7L, ID_CATEGORIA)));

        var nomina = asistenciaService.nomina(ID_SESION);

        assertThat(nomina.filas()).hasSize(2);
        assertThat(nomina.editable()).isTrue();
        // El que no escaneo aparece con estado nulo: es a quien hay que marcar.
        assertThat(nomina.filas())
                .anySatisfy(f -> assertThat(f.estado()).isEqualTo(Asistencia.ESTADO_PRESENTE))
                .anySatisfy(f -> assertThat(f.estado()).isNull());
    }

    @Test
    @DisplayName("la nomina de una sesion futura se puede ver pero no editar")
    void nomina_de_sesion_futura_no_es_editable() {
        var manana = LocalDate.now(Zonas.ECUADOR).plusDays(1);
        when(sesionRepository.findById(ID_SESION)).thenReturn(Optional.of(sesion(manana)));
        when(asistenciaRepository.findBySesionIdSesion(ID_SESION)).thenReturn(List.of());
        when(estudianteRepository.findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(ID_CATEGORIA))
                .thenReturn(List.of(estudiante(6L, ID_CATEGORIA)));

        var nomina = asistenciaService.nomina(ID_SESION);

        assertThat(nomina.editable()).isFalse();
        assertThat(nomina.motivoNoEditable()).contains("todavía no ocurre");
    }
}
