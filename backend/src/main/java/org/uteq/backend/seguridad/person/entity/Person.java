package org.uteq.backend.seguridad.person.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "personas", schema = "seguridad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    private Long idPersona;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    // RF-49 / H-01: opcional. La unicidad, cuando hay valor, la impone el
    // índice único parcial de V26 (no una constraint de columna, que en
    // PostgreSQL permitiría varias filas con el mismo valor sólo si es NULL).
    @Column(name = "cedula", length = 10)
    private String cedula;

    @Column(name = "correo", nullable = false, unique = true, length = 200)
    private String correo;

    @Column(name = "telefono", length = 15)
    private String telefono;

    @Column(name = "foto", columnDefinition = "text")
    private String foto;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "activo")
    private Boolean activo;

    // RNF-26 / H-09: doble opt-in del correo. El alta y el cambio de correo la
    // dejan en false y disparan un token de confirmación; RF-37 no envía el
    // enlace de restablecimiento a un correo no verificado. Las personas
    // preexistentes se dieron por verificadas en la migración V28.
    @Column(name = "correo_verificado", nullable = false)
    private Boolean correoVerificado;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.activo == null) this.activo = true;
        if (this.correoVerificado == null) this.correoVerificado = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
