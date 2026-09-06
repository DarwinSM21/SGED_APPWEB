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

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    boolean existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
            Long idEstudiante, TipoPago tipo, Short anio, Short mes);

    List<Payment> findByEstudiante_IdEstudianteOrderByFechaPagoDesc(Long idEstudiante);

    @Query("""
           SELECT COALESCE(SUM(p.monto), 0) FROM Payment p
            WHERE p.fechaPago BETWEEN :inicio AND :fin
              AND p.anuladoEn IS NULL
           """)
    BigDecimal sumAmountBetweenDates(LocalDate inicio, LocalDate fin);

    long countByFechaPagoBetweenAndAnuladoEnIsNull(LocalDate inicio, LocalDate fin);

    @Query("""
           SELECT p.estudiante.idEstudiante FROM Payment p
           WHERE p.tipo = :tipo AND p.anio = :anio AND p.mes = :mes
             AND p.anuladoEn IS NULL
           """)
    List<Long> idsWithMembershipCovered(
            @Param("tipo") TipoPago tipo, @Param("anio") Short anio, @Param("mes") Short mes);

    @Query("""
           SELECT year(p.fechaPago), month(p.fechaPago), SUM(p.monto), COUNT(p)
           FROM Payment p
           WHERE p.fechaPago BETWEEN :desde AND :hasta
             AND p.anuladoEn IS NULL
           GROUP BY year(p.fechaPago), month(p.fechaPago)
           """)
    List<Object[]> monthlyBillingTotals(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
