package org.uteq.backend.deportivo.injury.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.academico.guardian.service.NotificationService;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.coach.repository.CoachRepository;
import org.uteq.backend.deportivo.injury.entity.Injury;
import org.uteq.backend.deportivo.injury.repository.InjuryRepository;
import org.uteq.backend.seguridad.audit.aop.Audited;

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
public class InjuryService {
    private final InjuryRepository injuryRepository;
    private final StudentRepository estudianteRepository;
    private final CoachRepository coachRepository;
    private final NotificationService notificacionService;

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
     * @throws ResourceNotFoundException si el estudiante o el entrenador
     *                                      no existen
     * @throws IllegalArgumentException     si el estudiante ya tiene una
     *                                      lesión activa, o la fecha estimada
     *                                      de retorno es anterior a la de la
     *                                      lesión
     */
    @Audited(action = "CREAR", entity = "Injury", idSpel = "#result.idLesion",
            descriptionSpel = "'registró una lesión del estudiante #' + #p0")
    @Transactional
    public Injury register(Long idEstudiante, Long idEntrenador, String descripcion,
                            LocalDate fechaLesion, LocalDate fechaEstimadaRetorno) {
        var estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el estudiante " + idEstudiante));
        var entrenador = coachRepository.findById(idEntrenador)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el entrenador " + idEntrenador));

        // La base tiene un índice único parcial que impide dos lesiones activas
        // del mismo estudiante. Se comprueba antes para devolver un mensaje
        // útil en vez de un error de restricción.
        injuryRepository.findActiveByStudent(idEstudiante).ifPresent(l -> {
            throw new IllegalArgumentException(
                    "El estudiante ya tiene una lesion activa registrada el "
                            + l.getFechaLesion() + ". Da de alta esa antes de registrar otra.");
        });

        LocalDate fecha = fechaLesion != null ? fechaLesion : LocalDate.now(Zones.ECUADOR);
        if (fechaEstimadaRetorno != null && fechaEstimadaRetorno.isBefore(fecha)) {
            throw new IllegalArgumentException(
                    "La fecha estimada de retorno no puede ser anterior a la de la lesion");
        }

        Injury lesion = injuryRepository.save(Injury.builder()
                .estudiante(estudiante)
                .entrenador(entrenador)
                .descripcion(descripcion)
                .fechaLesion(fecha)
                .fechaEstimadaRetorno(fechaEstimadaRetorno)
                .build());
        notificacionService.notifyInjury(estudiante, descripcion);
        return lesion;
    }

    /**
     * Cierra una lesión: el jugador vuelve a entrar en las plantillas.
     *
     * @param idLesion  identificador de la lesión
     * @param fechaAlta fecha de alta; {@code null} usa hoy
     * @return la lesión dada de alta
     * @throws ResourceNotFoundException si la lesión no existe
     * @throws IllegalArgumentException     si ya tiene fecha de alta, o el
     *                                      alta es anterior a la fecha de la
     *                                      lesión
     */
    @Audited(action = "EDITAR", entity = "Injury", idSpel = "#result.idLesion",
            descriptionSpel = "'dio de alta la lesión #' + #result.idLesion")
    @Transactional
    public Injury discharge(Long idLesion, LocalDate fechaAlta) {
        var lesion = injuryRepository.findById(idLesion)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la lesion " + idLesion));

        if (lesion.getFechaAlta() != null) {
            throw new IllegalArgumentException("Esa lesion ya tiene fecha de alta");
        }

        LocalDate fecha = fechaAlta != null ? fechaAlta : LocalDate.now(Zones.ECUADOR);
        if (fecha.isBefore(lesion.getFechaLesion())) {
            throw new IllegalArgumentException(
                    "El alta no puede ser anterior a la fecha de la lesion");
        }

        lesion.setFechaAlta(fecha);
        return injuryRepository.save(lesion);
    }

    /**
     * Lista paginada de lesiones activas.
     *
     * @param pageable paginación y orden
     * @return la página de lesiones activas
     */
    @Transactional(readOnly = true)
    public Page<Injury> findActive(Pageable pageable) {
        return injuryRepository.findActive(pageable);
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
    public Page<Injury> historyOf(Long idEstudiante, Pageable pageable) {
        return injuryRepository.findByStudentOrderByInjuryDateDesc(idEstudiante, pageable);
    }

    /**
     * Identificadores de los estudiantes con una lesión activa (para el
     * panel de alertas y las sugerencias de plantilla).
     *
     * @return la lista de identificadores de estudiantes lesionados
     */
    @Transactional(readOnly = true)
    public List<Long> injuredIds() {
        return injuryRepository.injuredStudentIds();
    }
}
