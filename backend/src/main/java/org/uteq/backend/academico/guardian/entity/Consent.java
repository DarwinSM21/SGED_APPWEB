package org.uteq.backend.academico.guardian.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.academico.student.entity.Student;
import org.uteq.backend.seguridad.user.entity.UserAccount;

import java.time.OffsetDateTime;

@Entity
@Table(name = "consentimientos", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consent {
    public static final String ALCANCE_INFORMES = "INFORMES";
    public static final String ALCANCE_NOTIFICACIONES = "NOTIFICACIONES";
    public static final String ALCANCE_NOTIFICACIONES_ASISTENCIA = "NOTIFICACIONES_ASISTENCIA";
    public static final String ALCANCE_NOTIFICACIONES_LESION = "NOTIFICACIONES_LESION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consentimiento")
    private Long idConsentimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_representante", nullable = false)
    private Guardian representante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Student estudiante;

    @Column(name = "alcance", nullable = false, length = 50)
    private String alcance;

    @Column(name = "otorgado_en", nullable = false)
    private OffsetDateTime otorgadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id_usuario")
    private UserAccount registradoPor;

    @Column(name = "revocado_en")
    private OffsetDateTime revocadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revocado_por_id_usuario")
    private UserAccount revocadoPor;

    @Transient
    public boolean isActive() {
        return revocadoEn == null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.otorgadoEn == null) this.otorgadoEn = OffsetDateTime.now();
    }
}
