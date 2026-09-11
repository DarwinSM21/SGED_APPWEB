package org.uteq.backend.seguridad.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.role.entity.Role;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;

@Entity
@Table(name = "usuarios", schema = "seguridad")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_persona", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_general", nullable = false)
    private GeneralStatus generalStatus;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @Column(name = "ultimo_acceso")
    private OffsetDateTime lastAccess;

    @Column(name = "activo")
    private Boolean active;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_rol",
        schema = "seguridad",
        joinColumns = @JoinColumn(name = "id_usuario"),
        inverseJoinColumns = @JoinColumn(name = "id_rol")
    )
    private Set<Role> roles;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
        if (this.active == null) this.active = true;
        normalizeUsername();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
        normalizeUsername();
    }

    private void normalizeUsername() {
        if (this.username != null) {
            this.username = this.username.trim().toLowerCase(Locale.ROOT);
        }
    }
}
