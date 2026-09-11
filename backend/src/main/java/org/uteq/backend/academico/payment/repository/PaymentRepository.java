package org.uteq.backend.academico.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.payment.entity.Payment;
import org.uteq.backend.academico.payment.entity.Payment.PaymentType;

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
    @Query("""
           SELECT COUNT(p) > 0 FROM Payment p
           WHERE p.student.id = :idEstudiante AND p.type = :tipo AND p.year = :anio AND p.month = :mes
             AND p.canceledAt IS NULL
           """)
    boolean existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
            @Param("idEstudiante") Long idEstudiante, @Param("tipo") PaymentType tipo,
            @Param("anio") Short anio, @Param("mes") Short mes);

    /**
     * @param idEstudiante identificador del estudiante
     * @return los pagos de ese estudiante, del más reciente al más antiguo
     */
    @Query("SELECT p FROM Payment p WHERE p.student.id = :idEstudiante ORDER BY p.paymentDate DESC")
    List<Payment> findByEstudiante_IdEstudianteOrderByFechaPagoDesc(@Param("idEstudiante") Long idEstudiante);

    /**
     * @param inicio fecha inicial del rango, inclusive
     * @param fin fecha final del rango, inclusive
     * @return la suma de los montos no anulados pagados en ese rango de fechas
     */
    @Query("""
           SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            WHERE p.paymentDate BETWEEN :inicio AND :fin
              AND p.canceledAt IS NULL
           """)
    BigDecimal sumAmountBetweenDates(LocalDate inicio, LocalDate fin);

    /**
     * @param inicio fecha inicial del rango, inclusive
     * @param fin fecha final del rango, inclusive
     * @return la cantidad de pagos no anulados registrados en ese rango de fechas
     */
    @Query("""
           SELECT COUNT(p) FROM Payment p
           WHERE p.paymentDate BETWEEN :inicio AND :fin AND p.canceledAt IS NULL
           """)
    long countByFechaPagoBetweenAndAnuladoEnIsNull(@Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

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
           SELECT p.student.id FROM Payment p
           WHERE p.type = :tipo AND p.year = :anio AND p.month = :mes
             AND p.canceledAt IS NULL
           """)
    List<Long> idsWithMembershipCovered(
            @Param("tipo") PaymentType tipo, @Param("anio") Short anio, @Param("mes") Short mes);

    /**
     * Totales de facturación agrupados por año y mes, para el reporte de
     * ingresos.
     *
     * @param desde fecha inicial del rango, inclusive
     * @param hasta fecha final del rango, inclusive
     * @return filas {@code [año, mes, suma de montos, cantidad de pagos]} por período
     */
    @Query("""
           SELECT year(p.paymentDate), month(p.paymentDate), SUM(p.amount), COUNT(p)
           FROM Payment p
           WHERE p.paymentDate BETWEEN :desde AND :hasta
             AND p.canceledAt IS NULL
           GROUP BY year(p.paymentDate), month(p.paymentDate)
           """)
    List<Object[]> monthlyBillingTotals(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
