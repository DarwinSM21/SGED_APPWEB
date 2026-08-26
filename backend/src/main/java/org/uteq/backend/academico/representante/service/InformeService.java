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
 * Lectura de informes para el representante autenticado.
 *
 * <p>Recibe {@code username} como parametro plano en vez de leer
 * {@code SecurityContextHolder} aqui: el principal se resuelve siempre en
 * el controller (igual que {@code AuthController.me()} o
 * {@code SesionEntrenamientoController.hoy()}), asi este servicio se
 * prueba con un String cualquiera sin simular contexto de seguridad.
 *
 * <p>El chequeo de pertenencia (vinculo activo representante-estudiante)
 * es lo unico que autoriza esta lectura -no el consentimiento, ver la
 * nota bajo el hallazgo H-04 en ETHICS.md-, y responde 404 uniforme
 * tanto si el estudiante no existe como si no es un representado suyo:
 * distinguir los casos confirmaria que un id ajeno corresponde a un
 * estudiante real (mismo criterio que el canjeo de QR, que responde 410
 * parejo para "no existio" y "ya se uso").
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
     * Informe del propio ESTUDIANTE autenticado: mismas piezas que
     * informeDe() (promedio por criterio, historial de lesiones, % de
     * asistencia), pero sin el chequeo de vinculo -aqui la unica
     * autorizacion que hace falta es "es su propia cuenta"-.
     */
    @Transactional(readOnly = true)
    public InformeEstudianteResponse miInforme(String username) {
        Estudiante estudiante = estudianteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un estudiante asociado a esta cuenta"));
        return construirInforme(estudiante);
    }

    /**
     * El informe puesto en palabras.
     *
     * <p>Se pide aparte y no dentro de {@link #informeDe}: hablar con un
     * servicio externo tarda y puede fallar, y los numeros del informe tienen
     * que aparecer igual aunque el modelo este caido. Ademas asi no se gasta
     * cuota cada vez que alguien abre la pantalla, solo cuando la pide.
     *
     * <p>Se apoya en {@code informeDe} en lugar de repetir el chequeo de
     * pertenencia. No es por ahorrar lineas: si algun dia cambia la regla de
     * quien puede ver a quien, un segundo chequeo copiado aqui quedaria
     * desactualizado sin que nadie lo note, y eso es exactamente como se
     * filtran datos de un menor a quien no es su representante.
     */
    @Transactional(readOnly = true)
    public ComentarioInformeResponse comentarioDe(String username, Long idEstudiante) {
        InformeEstudianteResponse informe = informeDe(username, idEstudiante);
        return comentarSobre(informe);
    }

    /** Lo mismo para el estudiante que consulta su propio informe. */
    @Transactional(readOnly = true)
    public ComentarioInformeResponse miComentario(String username) {
        return comentarSobre(miInforme(username));
    }

    /**
     * Arma el perfil seudonimizado y pide el texto.
     *
     * <p>Al modelo va {@link PerfilJugadorAnonimo}, que no tiene nombre,
     * cedula, correo ni fecha de nacimiento -no existen esos campos-. Lo unico
     * que sale del sistema son promedios, categoria y cuantos entrenamientos
     * asistio. Es la misma restriccion que ya aplica la sugerencia de
     * alineacion, y aqui pesa mas: el titular de estos datos es un menor cuyo
     * representante todavia no tiene un consentimiento modelado (H-04).
     */
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
                // Referencia opaca: el modelo no necesita saber de quien habla.
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
