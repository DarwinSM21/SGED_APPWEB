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

    /**
     * Si el mes ya esta cubierto por un pago VIGENTE. Un pago anulado no
     * cubre nada: si contara, anular un cobro equivocado dejaria el mes
     * bloqueado para siempre y no se podria registrar el correcto.
     */
    boolean existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
            Long idEstudiante, TipoPago tipo, Short anio, Short mes);

    List<Pago> findByEstudiante_IdEstudianteOrderByFechaPagoDesc(Long idEstudiante);

    /**
     * Suma por fecha real de pago (no por el mes que una membresia cubre):
     * "ingresos del mes" es cuanto entro en caja este mes calendario, no
     * cuantos meses de membresia se cubrieron.
     */
    @Query("""
           SELECT COALESCE(SUM(p.monto), 0) FROM Pago p
            WHERE p.fechaPago BETWEEN :inicio AND :fin
              AND p.anuladoEn IS NULL
           """)
    BigDecimal sumarMontoEntreFechas(LocalDate inicio, LocalDate fin);

    long countByFechaPagoBetweenAndAnuladoEnIsNull(LocalDate inicio, LocalDate fin);

    /**
     * Quienes ya tienen cubierta la membresia de un mes concreto. Se pide la
     * lista completa de una vez, en lugar de preguntar estudiante por
     * estudiante: el panel de alertas la usa para restar sobre el total de
     * activos, y consultar de a uno serian tantas consultas como alumnos.
     */
    @Query("""
           SELECT p.estudiante.idEstudiante FROM Pago p
           WHERE p.tipo = :tipo AND p.anio = :anio AND p.mes = :mes
             AND p.anuladoEn IS NULL
           """)
    List<Long> idsConMembresiaCubierta(
            @Param("tipo") TipoPago tipo, @Param("anio") Short anio, @Param("mes") Short mes);

    /**
     * Recaudacion agrupada por mes calendario de cobro. Devuelve solo los
     * meses que tuvieron algun pago: los vacios los completa el servicio,
     * porque un mes sin ingresos tiene que dibujarse como barra en cero y no
     * desaparecer del grafico.
     */
    @Query("""
           SELECT year(p.fechaPago), month(p.fechaPago), SUM(p.monto), COUNT(p)
           FROM Pago p
           WHERE p.fechaPago BETWEEN :desde AND :hasta
             AND p.anuladoEn IS NULL
           GROUP BY year(p.fechaPago), month(p.fechaPago)
           """)
    List<Object[]> totalesPorMesDeCobro(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
