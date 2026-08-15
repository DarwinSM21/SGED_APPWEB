package org.uteq.backend.academico.pago.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.pago.entity.Pago;
import org.uteq.backend.academico.pago.entity.Pago.TipoPago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

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

    /** Reporte de pagos: filtros opcionales de estudiante y rango de fechas. */
    @Query("""
           SELECT p FROM Pago p
           WHERE (:idEstudiante IS NULL OR p.estudiante.idEstudiante = :idEstudiante)
             AND (:desde IS NULL OR p.fechaPago >= :desde)
             AND (:hasta IS NULL OR p.fechaPago <= :hasta)
           ORDER BY p.fechaPago DESC
           """)
    List<Pago> buscarParaReporte(@Param("idEstudiante") Long idEstudiante,
                                  @Param("desde") LocalDate desde,
                                  @Param("hasta") LocalDate hasta);
}
