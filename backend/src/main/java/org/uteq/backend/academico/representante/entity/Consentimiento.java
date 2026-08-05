package org.uteq.backend.academico.representante.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.seguridad.usuario.entity.Usuario;

import java.time.OffsetDateTime;

/**
 * Registro de que un representante autorizo el tratamiento de los datos
 * de un representado: fecha, alcance y quien lo registro (hallazgo H-04
 * de docs/etica/ETHICS.md).
 *
 * <p>Deliberadamente NO gatea la lectura de informes -eso lo autoriza
 * unicamente el vinculo activo {@link RepresentanteEstudiante}, que crea
 * un administrador-. Queda reservada para cuando exista el envio real de
 * notificaciones (RF-22), que todavia no esta construido. Ver la nota de
 * resolucion bajo H-04 en ETHICS.md.
 */
@Entity
@Table(name = "consentimientos", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consentimiento {

    public static final String ALCANCE_INFORMES = "INFORMES";
    public static final String ALCANCE_NOTIFICACIONES_ASISTENCIA = "NOTIFICACIONES_ASISTENCIA";
    public static final String ALCANCE_NOTIFICACIONES_LESION = "NOTIFICACIONES_LESION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consentimiento")
    private Long idConsentimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_representante", nullable = false)
    private Representante representante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "alcance", nullable = false, length = 50)
    private String alcance;

    @Column(name = "otorgado_en", nullable = false)
    private OffsetDateTime otorgadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id_usuario")
    private Usuario registradoPor;

    @Column(name = "revocado_en")
    private OffsetDateTime revocadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revocado_por_id_usuario")
    private Usuario revocadoPor;

    /** true si nadie lo ha revocado todavia. Mismo criterio que Lesion.estaActiva(). */
    @Transient
    public boolean estaVigente() {
        return revocadoEn == null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.otorgadoEn == null) this.otorgadoEn = OffsetDateTime.now();
    }
}
