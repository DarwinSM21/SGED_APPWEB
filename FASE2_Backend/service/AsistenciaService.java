package org.uteq.backend.deportivo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.deportivo.dto.AsistenciaRequest;
import org.uteq.backend.deportivo.dto.AsistenciaResponse;
import org.uteq.backend.deportivo.entity.Asistencia;
import org.uteq.backend.deportivo.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.repository.SesionEntrenamientoRepository;
import org.uteq.backend.estudiante.entity.Estudiante;
import org.uteq.backend.estudiante.repository.EstudianteRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final SesionEntrenamientoRepository sesionRepository;
    private final EstudianteRepository estudianteRepository;

    /**
     * Registrar o actualizar asistencia de un estudiante en una sesión
     * Regla: Solo se permite PRESENTE, TARDE, AUSENTE, JUSTIFICADO
     */
    public AsistenciaResponse registrarAsistencia(Long idSesion, Long idEstudiante, AsistenciaRequest request) {
        
        SesionEntrenamiento sesion = sesionRepository.findById(idSesion)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada"));
        
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado"));
        
        // Buscar si ya existe asistencia
        Asistencia asistencia = asistenciaRepository
                .findBySesionIdSesionAndEstudianteIdSesion(idSesion, idEstudiante)
                .orElse(null);
        
        if (asistencia == null) {
            asistencia = Asistencia.builder()
                    .sesion(sesion)
                    .estudiante(estudiante)
                    .estado(Asistencia.EstadoAsistencia.valueOf(request.getEstado()))
                    .metodo(Asistencia.MetodoAsistencia.valueOf(request.getMetodo()))
                    .horaEntrada(request.getHoraEntrada())
                    .observacion(request.getObservacion())
                    .build();
        } else {
            // Actualizar existente
            asistencia.setEstado(Asistencia.EstadoAsistencia.valueOf(request.getEstado()));
            asistencia.setMetodo(Asistencia.MetodoAsistencia.valueOf(request.getMetodo()));
            asistencia.setHoraEntrada(request.getHoraEntrada());
            asistencia.setObservacion(request.getObservacion());
        }
        
        asistencia = asistenciaRepository.save(asistencia);
        return mapToResponse(asistencia);
    }

    /**
     * Obtener todas las asistencias de una sesión
     */
    @Transactional(readOnly = true)
    public List<AsistenciaResponse> obtenerAsistenciasPorSesion(Long idSesion) {
        return asistenciaRepository.findBySesionIdSesion(idSesion)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Obtener historial de asistencias de un estudiante
     */
    @Transactional(readOnly = true)
    public Page<AsistenciaResponse> obtenerHistoricoEstudiante(Long idEstudiante, Pageable pageable) {
        return asistenciaRepository.findByEstudianteIdEstudiante(idEstudiante, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Verificar si un estudiante puede ser calificado (PRESENTE o TARDE)
     */
    @Transactional(readOnly = true)
    public boolean puedeSerCalificado(Long idSesion, Long idEstudiante) {
        Asistencia asistencia = asistenciaRepository
                .findBySesionIdSesionAndEstudianteIdSesion(idSesion, idEstudiante)
                .orElse(null);
        
        if (asistencia == null) {
            return false;
        }
        
        return asistencia.getEstado() == Asistencia.EstadoAsistencia.PRESENTE ||
               asistencia.getEstado() == Asistencia.EstadoAsistencia.TARDE;
    }

    /**
     * Obtener asistencia específica
     */
    @Transactional(readOnly = true)
    public AsistenciaResponse obtenerAsistencia(Long idAsistencia) {
        return asistenciaRepository.findById(idAsistencia)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Asistencia no encontrada"));
    }

    /**
     * Mapear entidad a DTO de respuesta
     */
    private AsistenciaResponse mapToResponse(Asistencia asistencia) {
        return AsistenciaResponse.builder()
                .idAsistencia(asistencia.getIdAsistencia())
                .idSesion(asistencia.getSesion().getIdSesion())
                .idEstudiante(asistencia.getEstudiante().getIdEstudiante())
                .horaEntrada(asistencia.getHoraEntrada())
                .metodo(asistencia.getMetodo().toString())
                .estado(asistencia.getEstado().toString())
                .observacion(asistencia.getObservacion())
                .build();
    }
}
