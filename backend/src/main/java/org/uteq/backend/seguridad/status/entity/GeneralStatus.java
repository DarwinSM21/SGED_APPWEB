package org.uteq.backend.seguridad.status.entity;

import jakarta.persistence.*;
import lombok.*;

/** Estado general del catálogo administrativo (p. ej. activo, inactivo). */
@Entity
@Table(name = "estados_general", schema = "seguridad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralStatus {

    @Id
    @Column(name = "id_estado_general")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 50)
    private String name;
}
