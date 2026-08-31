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
    boolean existsByEstudiante_IdEstudianteAndTipoAndAnioAndMesAndAnuladoEnIsNull(
            Long idEstudiante, TipoPago tipo, Short anio, Short mes);

    List<Pago> findByEstudiante_IdEstudianteOrderByFechaPagoDesc(Long idEstudiante);

    @Query("""
           SELECT COALESCE(SUM(p.monto), 0) FROM Pago p
            WHERE p.fechaPago BETWEEN :inicio AND :fin
              AND p.anuladoEn IS NULL
           """)
    BigDecimal sumarMontoEntreFechas(LocalDate inicio, LocalDate fin);

    long countByFechaPagoBetweenAndAnuladoEnIsNull(LocalDate inicio, LocalDate fin);

    @Query("""
           SELECT p.estudiante.idEstudiante FROM Pago p
           WHERE p.tipo = :tipo AND p.anio = :anio AND p.mes = :mes
             AND p.anuladoEn IS NULL
           """)
    List<Long> idsConMembresiaCubierta(
            @Param("tipo") TipoPago tipo, @Param("anio") Short anio, @Param("mes") Short mes);

    @Query("""
           SELECT year(p.fechaPago), month(p.fechaPago), SUM(p.monto), COUNT(p)
           FROM Pago p
           WHERE p.fechaPago BETWEEN :desde AND :hasta
             AND p.anuladoEn IS NULL
           GROUP BY year(p.fechaPago), month(p.fechaPago)
           """)
    List<Object[]> totalesPorMesDeCobro(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
