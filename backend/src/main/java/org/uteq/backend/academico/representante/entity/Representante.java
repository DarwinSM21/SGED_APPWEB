package org.uteq.backend.academico.representante.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.seguridad.persona.entity.Persona;

@Entity
@Table(name = "representantes", schema = "academico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Representante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_representante")
    private Long idRepresentante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Persona persona;

    @Column(name = "ocupacion", length = 255)
    private String ocupacion;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = true;
}
