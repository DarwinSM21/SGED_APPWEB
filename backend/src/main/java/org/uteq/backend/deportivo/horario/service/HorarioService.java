package org.uteq.backend.deportivo.horario.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.entrenador.entity.Entrenador;
import org.uteq.backend.deportivo.entrenador.repository.EntrenadorRepository;
import org.uteq.backend.deportivo.horario.dto.HorarioRequest;
import org.uteq.backend.deportivo.horario.dto.HorarioResponse;
import org.uteq.backend.deportivo.horario.entity.Horario;
import org.uteq.backend.deportivo.horario.repository.HorarioRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final EntrenadorRepository entrenadorRepository;
    private final CategoriaRepository categoriaRepository;
    private final SesionEntrenamientoRepository sesionRepository;

    @Transactional
    public HorarioResponse crear(String username, HorarioRequest request) {
        Entrenador entrenador = entrenadorAutenticado(username);

        if (!request.horaFin().isAfter(request.horaInicio())) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoria no encontrada con id: " + request.idCategoria()));

        Horario horario = Horario.builder()
                .entrenador(entrenador)
                .categoria(categoria)
                .diaSemana(request.diaSemana().shortValue())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .campo(request.campo())
                .descripcion(request.descripcion())
                .activo(true)
                .build();

        return aResponse(horarioRepository.save(horario));
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> misHorarios(String username) {
        return entrenadorRepository.findByUsuario_Username(username)
                .map(entrenador -> horarioRepository
                        .findByEntrenador_IdEntrenadorAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(entrenador.getIdEntrenador())
                        .stream().map(this::aResponse).toList())
                .orElseGet(List::of);
    }

    /** 404 uniforme si el horario no existe o no es suyo: mismo criterio IDOR del resto de la app. */
    @Transactional
    public void desactivar(String username, Long idHorario) {
        Entrenador entrenador = entrenadorAutenticado(username);
        Horario horario = horarioRepository
                .findByIdHorarioAndEntrenador_IdEntrenador(idHorario, entrenador.getIdEntrenador())
                .orElseThrow(() -> new RecursoNoEncontradoException("Horario no encontrado con id: " + idHorario));
        horario.setActivo(false);
        horarioRepository.save(horario);
    }

    /**
     * Genera hoy las sesiones que falten a partir de los horarios fijos
     * activos que caen en el dia de hoy. Idempotente a proposito: se llama
     * en cada GET /api/sesiones/hoy y /mias (ver SesionEntrenamientoController),
     * y si la sesion de ese horario ya existe para hoy no crea una segunda.
     */
    @Transactional
    public void generarSesionesDeHoy() {
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        short diaSemana = (short) hoy.getDayOfWeek().getValue();

        for (Horario horario : horarioRepository.findByActivoTrueAndDiaSemana(diaSemana)) {
            if (sesionRepository.existsByHorario_IdHorarioAndFecha(horario.getIdHorario(), hoy)) {
                continue;
            }
            sesionRepository.save(SesionEntrenamiento.builder()
                    .horario(horario)
                    .entrenador(horario.getEntrenador())
                    .categoria(horario.getCategoria())
                    .fecha(hoy)
                    .horaInicio(horario.getHoraInicio())
                    .horaFin(horario.getHoraFin())
                    .campo(horario.getCampo())
                    .estado("PROGRAMADA")
                    .build());
        }
    }

    private Entrenador entrenadorAutenticado(String username) {
        return entrenadorRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un entrenador asociado a esta cuenta"));
    }

    private HorarioResponse aResponse(Horario h) {
        return new HorarioResponse(
                h.getIdHorario(), h.getCategoria().getNombre(), h.getDiaSemana().intValue(),
                h.getHoraInicio(), h.getHoraFin(), h.getCampo(), h.getDescripcion(), h.getActivo());
    }
}
