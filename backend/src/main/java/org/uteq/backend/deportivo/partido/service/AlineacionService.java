package org.uteq.backend.deportivo.partido.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.evaluacion.entity.Alineacion;
import org.uteq.backend.deportivo.evaluacion.entity.AlineacionJugador;
import org.uteq.backend.deportivo.evaluacion.repository.AlineacionRepository;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.deportivo.partido.dto.AlineacionDtos.GuardarAlineacionRequest;
import org.uteq.backend.deportivo.partido.dto.AlineacionDtos.JugadorEnCancha;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.AlineacionResponse;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.FeedbackAlineacionResponse;
import org.uteq.backend.deportivo.partido.dto.ConvocatoriaDtos.JugadorConvocado;
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.partido.service.ConvocatoriaService.Convocatoria;
import org.uteq.backend.deportivo.posicion.entity.Posicion;
import org.uteq.backend.deportivo.posicion.repository.PosicionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlineacionService {
    private final AlineacionRepository alineacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final PosicionRepository posicionRepository;
    private final LesionRepository lesionRepository;
    private final ConvocatoriaService convocatoriaService;

    @Value("${plantilla.titulares:11}")
    private int cupoTitulares;

    @Transactional(readOnly = true)
    public AlineacionResponse ver(Long idPartido) {
        Convocatoria convocatoria = convocatoriaService.calcular(idPartido);
        return alineacionRepository.findByPartido_IdPartido(idPartido)
                .map(a -> desdeGuardada(convocatoria, a))
                .orElseGet(() -> desdeSugerencia(convocatoria));
    }

    @Transactional
    public AlineacionResponse guardar(Long idPartido, GuardarAlineacionRequest request) {
        Convocatoria convocatoria = convocatoriaService.calcular(idPartido);
        Partido partido = convocatoria.partido();
        Long idCategoria = partido.getCategoria().getIdCategoria();

        Set<Long> lesionados = new HashSet<>(lesionRepository.idsEstudiantesLesionados());
        Set<Long> vistos = new LinkedHashSet<>();
        Map<Long, Long> puestoOcupado = new HashMap<>();
        int titulares = 0;

        List<AlineacionJugador> nuevos = new ArrayList<>();
        for (JugadorEnCancha j : request.jugadores()) {
            if (!vistos.add(j.idEstudiante())) {
                throw new IllegalArgumentException("Un jugador no puede estar dos veces en la alineación");
            }

            Estudiante estudiante = estudianteRepository.findByIdEstudianteAndActivoTrue(j.idEstudiante())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Estudiante no encontrado o inactivo: " + j.idEstudiante()));
            String nombre = ConvocatoriaService.nombreDe(estudiante);

            Long categoriaDelJugador = estudiante.getCategoria() == null
                    ? null : estudiante.getCategoria().getIdCategoria();
            if (!idCategoria.equals(categoriaDelJugador)) {
                throw new IllegalArgumentException(
                        nombre + " no pertenece a la categoría " + partido.getCategoria().getNombre());
            }

            if (lesionados.contains(j.idEstudiante())) {
                throw new IllegalArgumentException(nombre + " arrastra una lesión activa y no puede jugar");
            }

            boolean esTitular = Boolean.TRUE.equals(j.titular());
            Posicion posicion = null;
            if (j.idPosicion() != null) {
                posicion = posicionRepository.findById(j.idPosicion())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Posicion no encontrada: " + j.idPosicion()));
                if (esTitular && puestoOcupado.put(j.idPosicion(), j.idEstudiante()) != null) {
                    throw new IllegalArgumentException(
                            "Dos titulares no pueden ocupar el puesto " + posicion.getAbreviatura());
                }
            }
            if (esTitular && ++titulares > cupoTitulares) {
                throw new IllegalArgumentException(
                        "No podés poner más de " + cupoTitulares + " titulares en la cancha");
            }

            nuevos.add(AlineacionJugador.builder()
                    .estudiante(estudiante)
                    .posicion(posicion)
                    .titular(esTitular)
                    .build());
        }

        Alineacion alineacion = alineacionRepository.findByPartido_IdPartido(idPartido)
                .orElseGet(() -> Alineacion.builder().partido(partido).build());
        alineacion.setValoracion(request.valoracion());
        alineacion.setObservacion(request.observacion());

        if (!alineacion.getJugadores().isEmpty()) {
            alineacion.getJugadores().clear();
            alineacionRepository.saveAndFlush(alineacion);
        }
        for (AlineacionJugador j : nuevos) {
            j.setAlineacion(alineacion);
            alineacion.getJugadores().add(j);
        }
        alineacionRepository.save(alineacion);

        return ver(idPartido);
    }

    @Transactional
    public AlineacionResponse restablecer(Long idPartido) {
        alineacionRepository.findByPartido_IdPartido(idPartido).ifPresent(alineacionRepository::delete);
        return ver(idPartido);
    }

    @Transactional(readOnly = true)
    public FeedbackAlineacionResponse feedback(Long idPartido) {
        AlineacionResponse actual = ver(idPartido);
        if (actual.titulares().isEmpty()) {
            return new FeedbackAlineacionResponse(null, false, "No hay alineación que comentar");
        }
        var resultado = convocatoriaService.comentar(actual.titulares(), actual.categoria());
        return new FeedbackAlineacionResponse(
                resultado.texto(), resultado.disponible(), resultado.motivo());
    }

    private AlineacionResponse desdeGuardada(Convocatoria c, Alineacion a) {
        Map<Long, BigDecimal> promedios = c.promedios();
        Map<Long, Long> presencias = c.presencias();

        List<JugadorConvocado> titulares = new ArrayList<>();
        List<JugadorConvocado> suplentes = new ArrayList<>();
        Set<Long> yaEstan = new HashSet<>();

        for (AlineacionJugador j : a.getJugadores()) {
            Estudiante e = j.getEstudiante();
            yaEstan.add(e.getIdEstudiante());
            boolean titular = Boolean.TRUE.equals(j.getTitular());
            Long idPosicion = j.getPosicion() == null ? null : j.getPosicion().getIdPosicion();
            JugadorConvocado fila = convocatoriaService.aConvocado(
                    e, idPosicion, titular, promedios, presencias, c.entrenamientos());

            if (j.getPosicion() != null) {
                fila = new JugadorConvocado(fila.idEstudiante(), fila.nombreCompleto(),
                        j.getPosicion().getAbreviatura(), idPosicion, titular,
                        fila.promedio(), fila.presencias(), fila.entrenamientos());
            }
            (titular ? titulares : suplentes).add(fila);
        }

        return respuesta(c, true, a.getValoracion(), a.getObservacion(),
                titulares, suplentes, yaEstan);
    }

    private AlineacionResponse desdeSugerencia(Convocatoria c) {
        Set<Long> yaEstan = new HashSet<>();
        c.titulares().forEach(t -> yaEstan.add(t.idEstudiante()));
        c.suplentes().forEach(s -> yaEstan.add(s.idEstudiante()));
        return respuesta(c, false, null, null, c.titulares(), c.suplentes(), yaEstan);
    }

    private AlineacionResponse respuesta(Convocatoria c, boolean guardada, Short valoracion,
                                         String observacion, List<JugadorConvocado> titulares,
                                         List<JugadorConvocado> suplentes, Set<Long> yaEstan) {
        List<JugadorConvocado> disponibles = new ArrayList<>();
        for (JugadorConvocado j : c.titulares()) {
            if (!yaEstan.contains(j.idEstudiante())) {
                disponibles.add(sinTitularidad(j));
            }
        }
        for (JugadorConvocado j : c.suplentes()) {
            if (!yaEstan.contains(j.idEstudiante())) {
                disponibles.add(sinTitularidad(j));
            }
        }

        Partido p = c.partido();
        return new AlineacionResponse(
                p.getIdPartido(), p.getCategoria().getIdCategoria(), p.getCategoria().getNombre(),
                p.getFecha(), guardada, valoracion, observacion, c.ventana(),
                titulares, suplentes, disponibles, c.noConvocables(), cupoTitulares);
    }

    private JugadorConvocado sinTitularidad(JugadorConvocado j) {
        return new JugadorConvocado(j.idEstudiante(), j.nombreCompleto(), j.posicion(),
                j.idPosicion(), false, j.promedio(), j.presencias(), j.entrenamientos());
    }
}
