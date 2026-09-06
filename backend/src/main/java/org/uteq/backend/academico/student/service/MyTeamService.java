package org.uteq.backend.academico.student.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.student.dto.MyTeamDtos.*;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.academico.student.repository.StudentRepository;
import org.uteq.backend.common.Zones;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * "Mi equipo" del {@code ESTUDIANTE} autenticado: categoría, posición
 * nominal, entrenador de su próxima sesión programada y compañeros de la
 * misma categoría.
 */
@Service
@RequiredArgsConstructor
public class MyTeamService {
    private final StudentRepository estudianteRepository;
    private final SesionEntrenamientoRepository sesionRepository;

    /**
     * Arma la vista de equipo del estudiante dueño de la cuenta indicada.
     *
     * @param username nombre de usuario del estudiante autenticado
     * @return categoría, posición, entrenador de la próxima sesión y
     *         compañeros de categoría
     * @throws ResourceNotFoundException si la cuenta no tiene un estudiante
     *                                      asociado
     */
    @Transactional(readOnly = true)
    public MyTeamResponse myTeam(String username) {
        Student estudiante = estudianteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un estudiante asociado a esta cuenta"));

        var categoria = estudiante.getCategoria();
        var categoriaResponse = new CategoryDetailResponse(
                categoria.getNombre(),
                categoria.getEdadMin() == null ? null : categoria.getEdadMin().intValue(),
                categoria.getEdadMax() == null ? null : categoria.getEdadMax().intValue(),
                categoria.getDescripcion());

        var posicion = estudiante.getPosicion();
        PositionResponse posicionResponse = posicion == null ? null
                : new PositionResponse(posicion.getNombre(), posicion.getAbreviatura());

        AssignedCoachResponse entrenadorResponse = proximoEntrenadorDe(categoria.getIdCategoria());

        List<TeammateResponse> companeros = estudianteRepository
                .findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(
                        categoria.getIdCategoria(), estudiante.getIdEstudiante())
                .stream()
                .map(this::aCompanero)
                .toList();

        return new MyTeamResponse(categoriaResponse, posicionResponse, entrenadorResponse, companeros);
    }

    private AssignedCoachResponse proximoEntrenadorDe(Long idCategoria) {
        LocalDate hoy = LocalDate.now(Zones.ECUADOR);
        List<SesionEntrenamiento> proximas = sesionRepository
                .findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                        idCategoria, hoy, PageRequest.of(0, 1));
        if (proximas.isEmpty()) {
            return null;
        }
        var entrenador = proximas.get(0).getEntrenador();
        var persona = entrenador.getPersona();
        String especialidad = entrenador.getEspecialidad() == null ? null : entrenador.getEspecialidad().getNombre();
        return new AssignedCoachResponse(persona.getNombre() + " " + persona.getApellido(), especialidad);
    }

    private TeammateResponse aCompanero(Student e) {
        var persona = e.getPersona();
        String posicion = e.getPosicion() == null ? null : e.getPosicion().getNombre();
        return new TeammateResponse(e.getIdEstudiante(), persona.getNombre() + " " + persona.getApellido(), posicion);
    }
}
