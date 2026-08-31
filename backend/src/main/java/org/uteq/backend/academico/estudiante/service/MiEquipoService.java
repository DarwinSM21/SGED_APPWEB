package org.uteq.backend.academico.estudiante.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.academico.estudiante.dto.MiEquipoDtos.*;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.estudiante.repository.EstudianteRepository;
import org.uteq.backend.common.Zonas;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;
import org.uteq.backend.deportivo.sesion.repository.SesionEntrenamientoRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MiEquipoService {
    private final EstudianteRepository estudianteRepository;
    private final SesionEntrenamientoRepository sesionRepository;

    @Transactional(readOnly = true)
    public MiEquipoResponse miEquipo(String username) {
        Estudiante estudiante = estudianteRepository.findByUsuario_Username(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un estudiante asociado a esta cuenta"));

        var categoria = estudiante.getCategoria();
        var categoriaResponse = new CategoriaDetalleResponse(
                categoria.getNombre(),
                categoria.getEdadMin() == null ? null : categoria.getEdadMin().intValue(),
                categoria.getEdadMax() == null ? null : categoria.getEdadMax().intValue(),
                categoria.getDescripcion());

        var posicion = estudiante.getPosicion();
        PosicionResponse posicionResponse = posicion == null ? null
                : new PosicionResponse(posicion.getNombre(), posicion.getAbreviatura());

        EntrenadorAsignadoResponse entrenadorResponse = proximoEntrenadorDe(categoria.getIdCategoria());

        List<CompaneroResponse> companeros = estudianteRepository
                .findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(
                        categoria.getIdCategoria(), estudiante.getIdEstudiante())
                .stream()
                .map(this::aCompanero)
                .toList();

        return new MiEquipoResponse(categoriaResponse, posicionResponse, entrenadorResponse, companeros);
    }

    private EntrenadorAsignadoResponse proximoEntrenadorDe(Long idCategoria) {
        LocalDate hoy = LocalDate.now(Zonas.ECUADOR);
        List<SesionEntrenamiento> proximas = sesionRepository
                .findByCategoriaIdCategoriaAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(
                        idCategoria, hoy, PageRequest.of(0, 1));
        if (proximas.isEmpty()) {
            return null;
        }
        var entrenador = proximas.get(0).getEntrenador();
        var persona = entrenador.getPersona();
        String especialidad = entrenador.getEspecialidad() == null ? null : entrenador.getEspecialidad().getNombre();
        return new EntrenadorAsignadoResponse(persona.getNombre() + " " + persona.getApellido(), especialidad);
    }

    private CompaneroResponse aCompanero(Estudiante e) {
        var persona = e.getPersona();
        String posicion = e.getPosicion() == null ? null : e.getPosicion().getNombre();
        return new CompaneroResponse(e.getIdEstudiante(), persona.getNombre() + " " + persona.getApellido(), posicion);
    }
}
