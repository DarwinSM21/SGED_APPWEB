package org.uteq.backend.deportivo.lesion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.service.NotificacionService;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;

import java.time.LocalDate;
import java.util.List;

/**
 * Registro de lesiones. Las carga el entrenador desde el módulo de
 * evaluación diaria. Una lesión activa tiene tres efectos: excluye al
 * jugador de las sugerencias de plantilla, distingue su ausencia de una
 * falta sin motivo, y dispara la notificación al representante.
 */
@Service
@RequiredArgsConstructor
public class LesionService {
    private final LesionRepository lesionRepository;
    private final EstudianteRepository estudianteRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final NotificacionService notificacionService;

    /**
     * Registra una lesión y notifica a los representantes del estudiante.
     *
     * @param idEstudiante         estudiante lesionado
     * @param idEntrenador         entrenador que registra
     * @param descripcion          descripción de la lesión (dato de salud)
     * @param fechaLesion          fecha de la lesión; {@code null} usa hoy
     * @param fechaEstimadaRetorno fecha estimada de retorno; puede ser
     *                             {@code null}
     * @return la lesión registrada
     * @throws RecursoNoEncontradoException si el estudiante o el entrenador
     *                                      no existen
     * @throws IllegalArgumentException     si el estudiante ya tiene una
     *                                      lesión activa, o la fecha estimada
     *                                      de retorno es anterior a la de la
     *                                      lesión
     */
    @Auditado(accion = "CREAR", entidad = "Lesion", idSpel = "#result.idLesion",
            descripcionSpel = "'registró una lesión del estudiante #' + #p0")
    @Transactional
    public Lesion registrar(Long idEstudiante, Long idEntrenador, String descripcion,
                            LocalDate fechaLesion, LocalDate fechaEstimadaRetorno) {
        var estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el estudiante " + idEstudiante));
        var entrenador = entrenadorRepository.findById(idEntrenador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el entrenador " + idEntrenador));

        // La base tiene un índice único parcial que impide dos lesiones activas
        // del mismo estudiante. Se comprueba antes para devolver un mensaje
        // útil en vez de un error de restricción.
        lesionRepository.buscarActivaPorEstudiante(idEstudiante).ifPresent(l -> {
            throw new IllegalArgumentException(
                    "El estudiante ya tiene una lesion activa registrada el "
                            + l.getFechaLesion() + ". Da de alta esa antes de registrar otra.");
        });

        LocalDate fecha = fechaLesion != null ? fechaLesion : LocalDate.now(Zonas.ECUADOR);
        if (fechaEstimadaRetorno != null && fechaEstimadaRetorno.isBefore(fecha)) {
            throw new IllegalArgumentException(
                    "La fecha estimada de retorno no puede ser anterior a la de la lesion");
        }

        Lesion lesion = lesionRepository.save(Lesion.builder()
                .estudiante(estudiante)
                .entrenador(entrenador)
                .descripcion(descripcion)
                .fechaLesion(fecha)
                .fechaEstimadaRetorno(fechaEstimadaRetorno)
                .build());
        notificacionService.notificarLesion(estudiante, descripcion);
        return lesion;
    }

    /**
     * Cierra una lesión: el jugador vuelve a entrar en las plantillas.
     *
     * @param idLesion  identificador de la lesión
     * @param fechaAlta fecha de alta; {@code null} usa hoy
     * @return la lesión dada de alta
     * @throws RecursoNoEncontradoException si la lesión no existe
     * @throws IllegalArgumentException     si ya tiene fecha de alta, o el
     *                                      alta es anterior a la fecha de la
     *                                      lesión
     */
    @Auditado(accion = "EDITAR", entidad = "Lesion", idSpel = "#result.idLesion",
            descripcionSpel = "'dio de alta la lesión #' + #result.idLesion")
    @Transactional
    public Lesion darDeAlta(Long idLesion, LocalDate fechaAlta) {
        var lesion = lesionRepository.findById(idLesion)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la lesion " + idLesion));

        if (lesion.getFechaAlta() != null) {
            throw new IllegalArgumentException("Esa lesion ya tiene fecha de alta");
        }

        LocalDate fecha = fechaAlta != null ? fechaAlta : LocalDate.now(Zonas.ECUADOR);
        if (fecha.isBefore(lesion.getFechaLesion())) {
            throw new IllegalArgumentException(
                    "El alta no puede ser anterior a la fecha de la lesion");
        }

        lesion.setFechaAlta(fecha);
        return lesionRepository.save(lesion);
    }

    /**
     * Lista paginada de lesiones activas.
     *
     * @param pageable paginación y orden
     * @return la página de lesiones activas
     */
    @Transactional(readOnly = true)
    public Page<Lesion> listarActivas(Pageable pageable) {
        return lesionRepository.listarActivas(pageable);
    }

    /**
     * Historial de lesiones de un estudiante, de la más reciente a la más
     * antigua.
     *
     * @param idEstudiante identificador del estudiante
     * @param pageable     paginación
     * @return la página de lesiones del estudiante
     */
    @Transactional(readOnly = true)
    public Page<Lesion> historialDe(Long idEstudiante, Pageable pageable) {
        return lesionRepository.findByEstudianteIdEstudianteOrderByFechaLesionDesc(idEstudiante, pageable);
    }

    /**
     * Identificadores de los estudiantes con una lesión activa (para el
     * panel de alertas y las sugerencias de plantilla).
     *
     * @return la lista de identificadores de estudiantes lesionados
     */
    @Transactional(readOnly = true)
    public List<Long> idsLesionados() {
        return lesionRepository.idsEstudiantesLesionados();
    }
}
