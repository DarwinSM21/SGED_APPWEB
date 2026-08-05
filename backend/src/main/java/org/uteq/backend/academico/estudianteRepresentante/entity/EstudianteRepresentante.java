package org.uteq.backend.academico.estudianteRepresentante.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.academico.estudiante.entity.Estudiante;
import org.uteq.backend.academico.representante.entity.Representante;

@Entity
@Table(name = "estudiante_representante", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstudianteRepresentante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante_representante")
    private Long idEstudianteRepresentante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_representante", nullable = false)
    private Representante representante;

    @Column(name = "relacion", nullable = false, length = 50)
    private String relacion;

    @Column(name = "contacto_principal")
    @Builder.Default
    private Boolean contactoPrincipal = false;
}
