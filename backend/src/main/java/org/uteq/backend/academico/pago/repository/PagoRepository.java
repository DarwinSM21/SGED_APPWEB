package org.uteq.backend.academico.pago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    boolean existsByEstudiante_IdEstudianteAndTipoAndAnioAndMes(
            Long idEstudiante, TipoPago tipo, Short anio, Short mes);

    List<Pago> findByEstudiante_IdEstudianteOrderByFechaPagoDesc(Long idEstudiante);

    /**
     * Suma por fecha real de pago (no por el mes que una membresia cubre):
     * "ingresos del mes" es cuanto entro en caja este mes calendario, no
     * cuantos meses de membresia se cubrieron.
     */
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.fechaPago BETWEEN :inicio AND :fin")
    BigDecimal sumarMontoEntreFechas(LocalDate inicio, LocalDate fin);

    long countByFechaPagoBetween(LocalDate inicio, LocalDate fin);

    /**
     * Quienes ya tienen cubierta la membresia de un mes concreto. Se pide la
     * lista completa de una vez, en lugar de preguntar estudiante por
     * estudiante: el panel de alertas la usa para restar sobre el total de
     * activos, y consultar de a uno serian tantas consultas como alumnos.
     */
    @Query("""
           SELECT p.estudiante.idEstudiante FROM Pago p
           WHERE p.tipo = :tipo AND p.anio = :anio AND p.mes = :mes
           """)
    List<Long> idsConMembresiaCubierta(
            @Param("tipo") TipoPago tipo, @Param("anio") Short anio, @Param("mes") Short mes);
}
