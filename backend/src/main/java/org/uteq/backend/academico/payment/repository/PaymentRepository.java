package org.uteq.backend.academico.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.payment.entity.Payment;
import org.uteq.backend.academico.payment.entity.Payment.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Acceso a los pagos de matrícula/mensualidad, incluida la agregación usada
 * en reportes de facturación.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    /**
     * Comprueba si ya existe un pago no anulado de un tipo, año y mes para
     * un estudiante (evita cobrar dos veces el mismo período).
     *
     * @param idEstudiante identificador del estudiante
     * @param tipo tipo de pago (ej. mensualidad)
     * @param anio año del período pagado
     * @param mes mes del período pagado
     * @return {@code true} si ya existe un pago vigente para ese período
     */
    boolean existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
            Long idEstudiante, TipoPago tipo, Short anio, Short mes);

    /**
     * @param idEstudiante identificador del estudiante
     * @return los pagos de ese estudiante, del más reciente al más antiguo
     */
    List<Payment> findByEstudiante_IdEstudianteOrderByFechaPagoDesc(Long idEstudiante);

    /**
     * @param inicio fecha inicial del rango, inclusive
     * @param fin fecha final del rango, inclusive
     * @return la suma de los montos no anulados pagados en ese rango de fechas
     */
    @Query("""
           SELECT COALESCE(SUM(p.monto), 0) FROM Payment p
            WHERE p.fechaPago BETWEEN :inicio AND :fin
              AND p.anuladoEn IS NULL
           """)
    BigDecimal sumAmountBetweenDates(LocalDate inicio, LocalDate fin);

    /**
     * @param inicio fecha inicial del rango, inclusive
     * @param fin fecha final del rango, inclusive
     * @return la cantidad de pagos no anulados registrados en ese rango de fechas
     */
    long countByFechaPagoBetweenAndAnuladoEnIsNull(LocalDate inicio, LocalDate fin);

    /**
     * Estudiantes cuya mensualidad de un tipo/año/mes ya está cubierta por un
     * pago no anulado.
     *
     * @param tipo tipo de pago
     * @param anio año del período
     * @param mes mes del período
     * @return identificadores de los estudiantes con ese período pagado
     */
    @Query("""
           SELECT p.estudiante.idEstudiante FROM Payment p
           WHERE p.tipo = :tipo AND p.anio = :anio AND p.mes = :mes
             AND p.anuladoEn IS NULL
           """)
    List<Long> idsWithMembershipCovered(
            @Param("tipo") TipoPago tipo, @Param("anio") Short anio, @Param("mes") Short mes);

    /**
     * Totales de facturación agrupados por año y mes, para el reporte de
     * ingresos.
     *
     * @param desde fecha inicial del rango, inclusive
     * @param hasta fecha final del rango, inclusive
     * @return filas {@code [año, mes, suma de montos, cantidad de pagos]} por período
     */
    @Query("""
           SELECT year(p.fechaPago), month(p.fechaPago), SUM(p.monto), COUNT(p)
           FROM Payment p
           WHERE p.fechaPago BETWEEN :desde AND :hasta
             AND p.anuladoEn IS NULL
           GROUP BY year(p.fechaPago), month(p.fechaPago)
           """)
    List<Object[]> monthlyBillingTotals(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
