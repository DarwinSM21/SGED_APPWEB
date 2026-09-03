package org.uteq.backend.academico.representante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.academico.representante.dto.InformeDtos.*;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.common.ia.GeneradorFeedbackIA;
import org.uteq.backend.common.ia.PerfilJugadorAnonimo;
import org.uteq.backend.deportivo.asistencia.repository.AsistenciaRepository;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lectura de informes para el representante o el estudiante autenticado.
 *
 * <p>Recibe {@code username} como parámetro plano en vez de leer el contexto
 * de seguridad aquí: el principal se resuelve siempre en el controlador, así
 * este servicio se prueba con un {@code String} cualquiera sin simular
 * contexto.
 *
 * <p>El chequeo de pertenencia (vínculo activo representante-estudiante) es
 * lo único que autoriza esta lectura, y responde {@code 404} uniforme tanto
 * si el estudiante no existe como si no es un representado suyo: distinguir
 * los casos confirmaría que un id ajeno corresponde a un estudiante real.
 */
@Service
@RequiredArgsConstructor
public class InformeService {
    private final RepresentanteRepository representanteRepository;
    private final RepresentanteEstudianteRepository vinculoRepository;
    private final EstudianteRepository estudianteRepository;
    private final LesionRepository lesionRepository;
    private final EvaluacionEstudianteRepository evaluacionEstudianteRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final GeneradorFeedbackIA generadorFeedback;

    /**
     * Lista de estudiantes a cargo del representante dueño de la cuenta.
     *
     * @param username nombre de usuario del representante autenticado
     * @return el resumen de cada representado
     * @throws RecursoNoEncontradoException si la cuenta no tiene un
     *                                      representante asociado
     */
    @Transactional(readOnly = true)
    public List<EstudianteResumenResponse> misRepresentados(String username) {
        Representante representante = representanteDe(username);
        return vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(representante.getIdRepresentante())
                .stream()
                .map(v -> {
                    Estudiante e = v.getEstudiante();
                    return new EstudianteResumenResponse(
                            e.getIdEstudiante(),
                            e.getPersona().getNombre() + " " + e.getPersona().getApellido(),
                            e.getCategoria().getNombre());
                })
                .toList();
    }

    /**
     * Informe de evaluación de un representado del representante autenticado.
     *
     * @param username     nombre de usuario del representante
     * @param idEstudiante identificador del estudiante
     * @return el informe (promedios por criterio, lesiones, % de asistencia)
     * @throws RecursoNoEncontradoException si la cuenta no tiene representante
     *                                      asociado, o el estudiante no
     *                                      existe o no es representado suyo
     */
    @Transactional(readOnly = true)
    public InformeEstudianteResponse informeDe(String username, Long idEstudiante) {
        Representante representante = representanteDe(username);

        boolean esSuyo = vinculoRepository.existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(
                representante.getIdRepresentante(), idEstudiante);
        if (!esSuyo) {
            throw new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante);
        }

        Estudiante estudiante = vinculoRepository
                .findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(representante.getIdRepresentante(), idEstudiante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Estudiante no encontrado con id: " + idEstudiante))
                .getEstudiante();

        return construirInforme(estudiante);
    }

    /**
     * Informe del propio estudiante autenticado: mismas piezas que
     * {@link #informeDe}, pero sin chequeo de vínculo —la única autorización
     * que hace falta es "es su propia cuenta"—.
     *
     * @param username nombre de usuario del estudiante autenticado
     * @return el informe del estudiante
     * @throws RecursoNoEncontradoException si la cuenta no tiene un estudiante
     *                                      asociado
     */
    @Transactional(readOnly = true)
    public InformeEstudianteResponse miInforme(String username) {
        Estudiante estudiante = estudianteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un estudiante asociado a esta cuenta"));
        return construirInforme(estudiante);
    }

    /**
     * El informe de un representado puesto en palabras por un servicio
     * externo. Se apoya en {@link #informeDe} en lugar de repetir el chequeo
     * de pertenencia: si cambia la regla de quién ve a quién, un segundo
     * chequeo copiado aquí quedaría desactualizado sin que nadie lo note.
     *
     * @param username     nombre de usuario del representante
     * @param idEstudiante identificador del estudiante
     * @return el comentario generado, o un texto por defecto si aún no hay
     *         evaluaciones
     * @throws RecursoNoEncontradoException si el estudiante no es representado
     *                                      del representante
     */
    @Transactional(readOnly = true)
    public ComentarioInformeResponse comentarioDe(String username, Long idEstudiante) {
        InformeEstudianteResponse informe = informeDe(username, idEstudiante);
        return comentarSobre(informe);
    }

    /**
     * Lo mismo que {@link #comentarioDe}, para el estudiante que consulta su
     * propio informe.
     *
     * @param username nombre de usuario del estudiante autenticado
     * @return el comentario generado, o un texto por defecto si aún no hay
     *         evaluaciones
     * @throws RecursoNoEncontradoException si la cuenta no tiene un estudiante
     *                                      asociado
     */
    @Transactional(readOnly = true)
    public ComentarioInformeResponse miComentario(String username) {
        return comentarSobre(miInforme(username));
    }

    // Arma el perfil seudonimizado y pide el texto. Al modelo va un
    // PerfilJugadorAnonimo, que no tiene nombre, cédula, correo ni fecha de
    // nacimiento: solo salen del sistema promedios, categoría y cuántos
    // entrenamientos asistió. El titular de estos datos es un menor.
    private ComentarioInformeResponse comentarSobre(InformeEstudianteResponse informe) {
        if (informe.promediosPorCriterio().isEmpty()) {
            return new ComentarioInformeResponse(null, false,
                    "Todavía no hay evaluaciones registradas para comentar");
        }

        Map<String, Double> promedios = new HashMap<>();
        for (PromedioCriterioResponse c : informe.promediosPorCriterio()) {
            promedios.put(c.criterio(), c.promedio());
        }

        boolean lesionado = informe.historialLesiones().stream()
                .anyMatch(LesionResumenResponse::activa);

        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        long asistencias = asistenciaRepository
                .contarAsistenciasDesde(informe.idEstudiante(), hoy.minusDays(30));

        var perfil = new PerfilJugadorAnonimo(
                "Jugador",
                informe.categoria(),
                null,
                promedios,
                Map.of(),
                (int) asistencias,
                lesionado);

        var resultado = generadorFeedback.generarComentarioJugador(perfil);
        return new ComentarioInformeResponse(
                resultado.texto(), resultado.disponible(), resultado.motivo());
    }

    private InformeEstudianteResponse construirInforme(Estudiante estudiante) {
        Long idEstudiante = estudiante.getIdEstudiante();

        List<PromedioCriterioResponse> promedios = evaluacionEstudianteRepository
                .promedioHistoricoPorCriterio(idEstudiante).stream()
                .map(fila -> new PromedioCriterioResponse(
                        (String) fila[0],
                        fila[1] == null ? 0.0 : ((Number) fila[1]).doubleValue()))
                .toList();

        List<LesionResumenResponse> lesiones = lesionRepository
                .findByEstudianteIdEstudianteOrderByFechaLesionDesc(idEstudiante, Pageable.unpaged())
                .getContent().stream()
                .map(this::aLesionResumen)
                .toList();

        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        BigDecimal porcentajeAsistencia = asistenciaRepository
                .calcularPorcentajeAsistencia(idEstudiante, hoy.minusDays(30), hoy);

        return new InformeEstudianteResponse(
                idEstudiante,
                estudiante.getPersona().getNombre() + " " + estudiante.getPersona().getApellido(),
                estudiante.getCategoria().getNombre(),
                promedios,
                lesiones,
                porcentajeAsistencia);
    }

    private Representante representanteDe(String username) {
        return representanteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un representante asociado a esta cuenta"));
    }

    private LesionResumenResponse aLesionResumen(Lesion l) {
        return new LesionResumenResponse(
                l.getIdLesion(), l.getDescripcion(), l.getFechaLesion(),
                l.getFechaEstimadaRetorno(), l.getFechaAlta(), l.estaActiva());
    }
}
