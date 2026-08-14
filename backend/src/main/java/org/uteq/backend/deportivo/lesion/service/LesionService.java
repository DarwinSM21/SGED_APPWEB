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

import java.time.LocalDate;
import java.util.List;

/**
 * Registro de lesiones. Las carga el entrenador desde el mismo modulo de
 * evaluacion diaria, no la recepcionista.
 *
 * <p>Una lesion activa tiene tres efectos en el sistema: excluye al jugador de
 * las sugerencias de plantilla, distingue su ausencia de una falta sin motivo,
 * y dispara la notificacion al representante.
 */
@Service
@RequiredArgsConstructor
public class LesionService {

    private final LesionRepository lesionRepository;
    private final EstudianteRepository estudianteRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final NotificacionService notificacionService;

    @Transactional
    public Lesion registrar(Long idEstudiante, Long idEntrenador, String descripcion,
                            LocalDate fechaLesion, LocalDate fechaEstimadaRetorno) {

        var estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el estudiante " + idEstudiante));
        var entrenador = entrenadorRepository.findById(idEntrenador)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el entrenador " + idEntrenador));

        // La base tiene un indice unico parcial que impide dos lesiones
        // activas del mismo estudiante. Se comprueba antes para devolver un
        // mensaje util en vez de un error de restriccion.
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

    /** Cierra una lesion: el jugador vuelve a entrar en las plantillas. */
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

    @Transactional(readOnly = true)
    public Page<Lesion> listarActivas(Pageable pageable) {
        return lesionRepository.listarActivas(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Lesion> historialDe(Long idEstudiante, Pageable pageable) {
        return lesionRepository.findByEstudianteIdEstudianteOrderByFechaLesionDesc(idEstudiante, pageable);
    }

    @Transactional(readOnly = true)
    public List<Long> idsLesionados() {
        return lesionRepository.idsEstudiantesLesionados();
    }
}
