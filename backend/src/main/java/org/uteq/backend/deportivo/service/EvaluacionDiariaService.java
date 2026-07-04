package org.uteq.backend.deportivo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.deportivo.dto.DetalleEvaluacionRequest;
import org.uteq.backend.deportivo.dto.EvaluacionDiariaRequest;
import org.uteq.backend.deportivo.dto.EvaluacionDiariaResponse;
import org.uteq.backend.deportivo.entity.*;
import org.uteq.backend.deportivo.repository.*;
import org.uteq.backend.estudiante.entity.Estudiante;
import org.uteq.backend.estudiante.repository.EstudianteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EvaluacionDiariaService {

    private final EvaluacionDiariaRepository evaluacionRepository;
    private final DetalleEvaluacionRepository detalleRepository;
    private final SesionEntrenamientoRepository sesionRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final EstudianteRepository estudianteRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final AsistenciaService asistenciaService;

    /**
     * Crear nueva evaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n diaria (cabecera)
     * Se crea en estado BORRADOR
     */
    public EvaluacionDiariaResponse crearEvaluacion(EvaluacionDiariaRequest request) {
        
        SesionEntrenamiento sesion = sesionRepository.findById(request.getIdSesion())
                .orElseThrow(() -> new IllegalArgumentException("SesiÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n no encontrada"));
        
        Entrenador entrenador = entrenadorRepository.findById(request.getIdEntrenador())
                .orElseThrow(() -> new IllegalArgumentException("Entrenador no encontrado"));
        
        // Validar que no exista evaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n para esta sesiÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n
        if (evaluacionRepository.findBySesionIdSesion(sesion.getIdSesion()).isPresent()) {
            throw new IllegalArgumentException("Ya existe una evaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n para esta sesiÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n");
        }
        
        EvaluacionDiaria evaluacion = EvaluacionDiaria.builder()
                .sesion(sesion)
                .entrenador(entrenador)
                .fecha(LocalDate.now())
                .observacionGeneral(request.getObservacionGeneral())
                .estado(EvaluacionDiaria.EstadoEvaluacion.BORRADOR)
                .build();
        
        evaluacion = evaluacionRepository.save(evaluacion);
        return mapToResponse(evaluacion);
    }

    /**
     * Agregar calificaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n a un estudiante
     * REGLA: Solo se pueden calificar estudiantes con estado PRESENTE o TARDE
     */
    public void agregarCalificacion(Long idEvaluacion, Long idEstudiante, DetalleEvaluacionRequest request) {
        
        EvaluacionDiaria evaluacion = evaluacionRepository.findById(idEvaluacion)
                .orElseThrow(() -> new IllegalArgumentException("EvaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n no encontrada"));
        
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        
        // VALIDAR: Solo calificar estudiantes con asistencia PRESENTE o TARDE
        if (!asistenciaService.puedeSerCalificado(evaluacion.getSesion().getIdSesion(), idEstudiante)) {
            throw new IllegalStateException(
                "Estudiante debe tener asistencia PRESENTE o TARDE para ser calificado"
            );
        }
        
        // Validar que la evaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n estÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â© en BORRADOR
        if (evaluacion.getEstado() != EvaluacionDiaria.EstadoEvaluacion.BORRADOR) {
            throw new IllegalStateException("Solo se pueden agregar calificaciones a evaluaciones en BORRADOR");
        }
        
        CriterioEvaluacion criterio = new CriterioEvaluacion();
        criterio.setIdCriterio(request.getIdCriterio());
        
        Posicion posicionJugada = null;
        if (request.getIdPosicionJugada() != null) {
            posicionJugada = new Posicion();
            posicionJugada.setIdPosicion(request.getIdPosicionJugada());
        }
        
        // Buscar si ya existe
        DetalleEvaluacion detalle = detalleRepository
                .findByEvaluacionIdEvaluacionAndEstudianteIdEstudianteAndCriterioIdCriterio(
                        idEvaluacion, idEstudiante, request.getIdCriterio()
                )
                .orElse(null);
        
        if (detalle == null) {
            detalle = DetalleEvaluacion.builder()
                    .evaluacion(evaluacion)
                    .estudiante(estudiante)
                    .criterio(criterio)
                    .posicionJugada(posicionJugada)
                    .puntaje(BigDecimal.valueOf(request.getPuntaje()))
                    .build();
        } else {
            detalle.setPuntaje(BigDecimal.valueOf(request.getPuntaje()));
        }
        
        detalleRepository.save(detalle);
        log.info("CalificaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n registrada: Eval={}, Est={}, Criterio={}, Puntaje={}",
                idEvaluacion, idEstudiante, request.getIdCriterio(), request.getPuntaje());
    }

    /**
     * Finalizar evaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n (cambiar estado de BORRADOR a FINALIZADA)
     */
    public EvaluacionDiariaResponse finalizarEvaluacion(Long idEvaluacion) {
        EvaluacionDiaria evaluacion = evaluacionRepository.findById(idEvaluacion)
                .orElseThrow(() -> new IllegalArgumentException("EvaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n no encontrada"));
        
        if (evaluacion.getEstado() != EvaluacionDiaria.EstadoEvaluacion.BORRADOR) {
            throw new IllegalStateException("Solo se pueden finalizar evaluaciones en BORRADOR");
        }
        
        evaluacion.setEstado(EvaluacionDiaria.EstadoEvaluacion.FINALIZADA);
        evaluacion = evaluacionRepository.save(evaluacion);
        
        log.info("EvaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n finalizada: {}", idEvaluacion);
        return mapToResponse(evaluacion);
    }

    /**
     * Obtener evaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n por ID
     */
    @Transactional(readOnly = true)
    public EvaluacionDiariaResponse obtenerEvaluacion(Long idEvaluacion) {
        return evaluacionRepository.findById(idEvaluacion)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("EvaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n no encontrada"));
    }

    /**
     * Obtener evaluaciones por entrenador (paginado)
     */
    @Transactional(readOnly = true)
    public Page<EvaluacionDiariaResponse> obtenerEvaluacionesPorEntrenador(Long idEntrenador, Pageable pageable) {
        return evaluacionRepository.findByEntrenadorIdEntrenador(idEntrenador, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Obtener promedio de evaluaciÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â³n por estudiante (para dashboard/IA)
     */
    @Transactional(readOnly = true)
    public BigDecimal obtenerPromedioEstudiante(Long idEvaluacion, Long idEstudiante) {
        return detalleRepository
                .findPromedioByEvaluacionAndEstudiante(idEvaluacion, idEstudiante)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Mapear a DTO de respuesta
     */
    private EvaluacionDiariaResponse mapToResponse(EvaluacionDiaria evaluacion) {
        return EvaluacionDiariaResponse.builder()
                .idEvaluacion(evaluacion.getIdEvaluacion())
                .idSesion(evaluacion.getSesion().getIdSesion())
                .idEntrenador(evaluacion.getEntrenador().getIdEntrenador())
                .creadoEn(LocalDateTime.ofInstant(evaluacion.getCreadoEn(), ZoneId.systemDefault()))
                .observacionGeneral(evaluacion.getObservacionGeneral())
                .estado(evaluacion.getEstado().toString())
                .build();
    }
}
