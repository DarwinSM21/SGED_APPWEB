package org.uteq.backend.academico.asistencia.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.asistencia.dto.AsistenciaRequest;
import org.uteq.backend.academico.asistencia.dto.AsistenciaResponse;
import org.uteq.backend.academico.asistencia.entity.Asistencia;
import org.uteq.backend.academico.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.estadoAsistencia.entity.EstadoAsistencia;
import org.uteq.backend.deportivo.estadoAsistencia.repository.EstadoAsistenciaRepository;
import org.uteq.backend.deportivo.sesionEntrenamiento.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesionEntrenamiento.repository.SesionEntrenamientoRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final SesionEntrenamientoRepository sesionEntrenamientoRepository;
    private final EstudianteRepository estudianteRepository;
    private final EstadoAsistenciaRepository estadoAsistenciaRepository;

    @Transactional(readOnly = true)
    public Page<AsistenciaResponse> listar(Pageable pageable) {
        return asistenciaRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AsistenciaResponse> listarPorEstudiante(Long idEstudiante) {
        return asistenciaRepository.findByEstudiante_IdEstudiante(idEstudiante).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AsistenciaResponse> listarPorSesion(Long idSesionEntrenamiento) {
        return asistenciaRepository.findBySesionEntrenamiento_IdSesionEntrenamiento(idSesionEntrenamiento).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AsistenciaResponse buscarPorId(Long id) {
        Asistencia a = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asistencia no encontrada con id: " + id));
        return toResponse(a);
    }

    @Transactional
    public AsistenciaResponse crear(AsistenciaRequest request) {
        SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(request.idSesionEntrenamiento())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Sesión de entrenamiento no encontrada con id: " + request.idSesionEntrenamiento()));

        Estudiante estudiante = estudianteRepository.findById(request.idEstudiante())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estudiante no encontrado con id: " + request.idEstudiante()));

        EstadoAsistencia estado = estadoAsistenciaRepository.findById(request.idEstadoAsistencia())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Estado de asistencia no encontrado con id: " + request.idEstadoAsistencia()));

        Asistencia asistencia = Asistencia.builder()
                .sesionEntrenamiento(sesion)
                .estudiante(estudiante)
                .estadoAsistencia(estado)
                .fechaRegistro(request.fechaRegistro() != null ? request.fechaRegistro() : LocalDate.now())
                .build();

        asistencia = asistenciaRepository.save(asistencia);
        return toResponse(asistencia);
    }

    @Transactional
    public AsistenciaResponse editar(Long id, AsistenciaRequest request) {
        Asistencia asistencia = asistenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Asistencia no encontrada con id: " + id));

        if (!asistencia.getSesionEntrenamiento().getIdSesionEntrenamiento().equals(request.idSesionEntrenamiento())) {
            SesionEntrenamiento sesion = sesionEntrenamientoRepository.findById(request.idSesionEntrenamiento())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Sesión de entrenamiento no encontrada con id: " + request.idSesionEntrenamiento()));
            asistencia.setSesionEntrenamiento(sesion);
        }

        if (!asistencia.getEstudiante().getIdEstudiante().equals(request.idEstudiante())) {
            Estudiante estudiante = estudianteRepository.findById(request.idEstudiante())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Estudiante no encontrado con id: " + request.idEstudiante()));
            asistencia.setEstudiante(estudiante);
        }

        if (!asistencia.getEstadoAsistencia().getIdEstadoAsistencia().equals(request.idEstadoAsistencia())) {
            EstadoAsistencia estado = estadoAsistenciaRepository.findById(request.idEstadoAsistencia())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Estado de asistencia no encontrado con id: " + request.idEstadoAsistencia()));
            asistencia.setEstadoAsistencia(estado);
        }

        if (request.fechaRegistro() != null) {
            asistencia.setFechaRegistro(request.fechaRegistro());
        }

        asistencia = asistenciaRepository.save(asistencia);
        return toResponse(asistencia);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!asistenciaRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Asistencia no encontrada con id: " + id);
        }
        asistenciaRepository.deleteById(id);
    }

    private AsistenciaResponse toResponse(Asistencia a) {
        return new AsistenciaResponse(
                a.getIdAsistencia(),
                a.getSesionEntrenamiento().getIdSesionEntrenamiento(),
                a.getSesionEntrenamiento().getTitulo(),
                a.getSesionEntrenamiento().getFecha(),
                a.getEstudiante().getIdEstudiante(),
                a.getEstudiante().getCodigoEstudiante(),
                a.getEstudiante().getPersona().getNombre(),
                a.getEstudiante().getPersona().getApellido(),
                a.getEstadoAsistencia().getIdEstadoAsistencia(),
                a.getEstadoAsistencia().getNombre(),
                a.getFechaRegistro()
        );
    }
}
