package org.uteq.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.representante.dto.InformeDtos.EstudianteResumenResponse;
import org.uteq.backend.academico.representante.dto.InformeDtos.InformeEstudianteResponse;
import org.uteq.backend.academico.representante.entity.Representante;
import org.uteq.backend.academico.representante.entity.RepresentanteEstudiante;
import org.uteq.backend.academico.representante.repository.RepresentanteEstudianteRepository;
import org.uteq.backend.academico.representante.repository.RepresentanteRepository;
import org.uteq.backend.academico.representante.service.InformeService;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.evaluacion.repository.EvaluacionEstudianteRepository;
import org.uteq.backend.deportivo.lesion.entity.Lesion;
import org.uteq.backend.deportivo.lesion.repository.LesionRepository;
import org.uteq.backend.seguridad.persona.entity.Persona;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * El caso que importa mas aqui es informeDe_lanza404_cuandoEstudianteNoEsSuyo:
 * prueba directamente que un representante no puede leer el informe de un
 * estudiante que no es suyo (IDOR/BOLA, OWASP A01) simplemente cambiando el
 * id en la URL. La autorizacion vertical (@PreAuthorize("hasRole('REPRESENTANTE')"))
 * no prueba esto -standaloneSetup ni siquiera evalua esa anotacion-, asi que
 * la unica prueba real de la autorizacion horizontal vive aqui, contra el
 * servicio directamente.
 */
@ExtendWith(MockitoExtension.class)
class InformeServiceTest {

    @Mock private RepresentanteRepository representanteRepository;
    @Mock private RepresentanteEstudianteRepository vinculoRepository;
    @Mock private LesionRepository lesionRepository;
    @Mock private EvaluacionEstudianteRepository evaluacionEstudianteRepository;

    @InjectMocks
    private InformeService informeService;

    private Representante representante() {
        return Representante.builder()
                .idRepresentante(1L)
                .persona(Persona.builder().nombre("Ana").apellido("Vera").build())
                .build();
    }

    private Estudiante estudiante(long id, String nombre) {
        return Estudiante.builder()
                .idEstudiante(id)
                .persona(Persona.builder().nombre(nombre).apellido("Hijo").build())
                .categoria(Categoria.builder().idCategoria(1L).nombre("SUB-12").build())
                .build();
    }

    @Test
    @DisplayName("misRepresentados lanza RecursoNoEncontradoException si la cuenta no tiene fila de representante")
    void misRepresentados_sin_representante_asociado_lanza_excepcion() {
        when(representanteRepository.findByUsuario_Username("huerfano@sged.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> informeService.misRepresentados("huerfano@sged.test"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("misRepresentados devuelve solo los vinculos activos del representante autenticado")
    void misRepresentados_devuelve_representados_activos() {
        Representante r = representante();
        RepresentanteEstudiante vinculo = RepresentanteEstudiante.builder()
                .representante(r).estudiante(estudiante(10L, "Juan")).activo(true).build();

        when(representanteRepository.findByUsuario_Username("ana.vera@sged.test")).thenReturn(Optional.of(r));
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndActivoTrue(1L)).thenReturn(List.of(vinculo));

        List<EstudianteResumenResponse> resultado = informeService.misRepresentados("ana.vera@sged.test");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombreCompleto()).isEqualTo("Juan Hijo");
    }

    @Test
    @DisplayName("informeDe responde 404 (RecursoNoEncontradoException) si el estudiante no es un representado suyo")
    void informeDe_lanza404_cuandoEstudianteNoEsSuyo() {
        Representante r = representante();
        when(representanteRepository.findByUsuario_Username("ana.vera@sged.test")).thenReturn(Optional.of(r));
        when(vinculoRepository.existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(1L, 999L))
                .thenReturn(false);

        assertThatThrownBy(() -> informeService.informeDe("ana.vera@sged.test", 999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("informeDe responde 404 si el vinculo existe pero fue desactivado (custodia revocada)")
    void informeDe_lanza404_cuandoVinculoEstaDesactivado() {
        Representante r = representante();
        when(representanteRepository.findByUsuario_Username("ana.vera@sged.test")).thenReturn(Optional.of(r));
        // existsBy...AndActivoTrue ya filtra por activo=true a nivel de query,
        // asi que un vinculo desactivado simplemente no cuenta como existente.
        when(vinculoRepository.existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(1L, 10L))
                .thenReturn(false);

        assertThatThrownBy(() -> informeService.informeDe("ana.vera@sged.test", 10L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("informeDe arma promedios y lesiones reutilizando las queries existentes cuando el estudiante si es suyo")
    void informeDe_devuelve_informe_de_un_representado_propio() {
        Representante r = representante();
        Estudiante hijo = estudiante(10L, "Juan");
        RepresentanteEstudiante vinculo = RepresentanteEstudiante.builder()
                .representante(r).estudiante(hijo).activo(true).build();
        Lesion lesion = Lesion.builder()
                .idLesion(5L).descripcion("Esguince").fechaLesion(LocalDate.of(2026, 1, 10))
                .fechaAlta(null).build();

        when(representanteRepository.findByUsuario_Username("ana.vera@sged.test")).thenReturn(Optional.of(r));
        when(vinculoRepository.existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(1L, 10L))
                .thenReturn(true);
        when(vinculoRepository.findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(1L, 10L))
                .thenReturn(Optional.of(vinculo));
        when(evaluacionEstudianteRepository.promedioHistoricoPorCriterio(10L))
                .thenReturn(List.<Object[]>of(new Object[]{"Tecnica", 7.5}));
        when(lesionRepository.findByEstudianteIdEstudianteOrderByFechaLesionDesc(any(), any()))
                .thenReturn((Page<Lesion>) new PageImpl<>(List.of(lesion)));

        InformeEstudianteResponse informe = informeService.informeDe("ana.vera@sged.test", 10L);

        assertThat(informe.nombreCompleto()).isEqualTo("Juan Hijo");
        assertThat(informe.promediosPorCriterio()).hasSize(1);
        assertThat(informe.promediosPorCriterio().get(0).criterio()).isEqualTo("Tecnica");
        assertThat(informe.promediosPorCriterio().get(0).promedio()).isEqualTo(7.5);
        assertThat(informe.historialLesiones()).hasSize(1);
        assertThat(informe.historialLesiones().get(0).activa()).isTrue();
    }
}
