package org.uteq.backend.academico.student.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.uteq.backend.academico.student.dto.EnableAccessRequest;
import org.uteq.backend.seguridad.auth.PasswordPolicy;
import org.uteq.backend.seguridad.status.entity.GeneralStatus;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;
import org.uteq.backend.seguridad.person.entity.Person;
import org.uteq.backend.seguridad.role.entity.Role;
import org.uteq.backend.seguridad.role.repository.RoleRepository;
import org.uteq.backend.seguridad.user.entity.UserAccount;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

import java.util.Set;

/**
 * Colaborador que concentra la relación {@code Student}–{@code UserAccount}:
 * la única porción de {@code StudentService} que cruzaba de lleno al
 * dominio de seguridad ({@code UserAccount}, {@code Role}, {@code PasswordEncoder}).
 * Extraído para bajar el fan-out de {@code StudentService} (hallazgo
 * MET-01 / R-06 del informe de evaluación de calidad). No orquesta el alta
 * completa —eso lo sigue llamando {@code StudentService}—, sino que aloja
 * el conocimiento de cómo se arma y valida una cuenta de rol
 * {@code ESTUDIANTE}.
 */
@Service
@RequiredArgsConstructor
public class StudentAccessService {
    private final UserAccountRepository usuarioRepository;
    private final RoleRepository rolRepository;
    private final GeneralStatusRepository estadoGeneralRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    /**
     * Guarda simétrica a {@code UserAccountService.validarRolCoherente}: si la
     * persona ya tiene cuenta, esa cuenta tiene que ser de estudiante. Sin
     * cuenta no hay nada que validar.
     *
     * @param idPersona persona sobre la que se quiere crear la ficha de
     *                  estudiante
     * @throws IllegalArgumentException si la persona tiene una cuenta activa
     *                                  con un rol distinto de {@code ESTUDIANTE}
     */
    public void validateConsistencyWithStudentRecord(Long idPersona) {
        usuarioRepository.findByPerson_IdAndActiveTrue(idPersona).ifPresent(usuario -> {
            boolean esEstudiante = usuario.getRoles() != null && usuario.getRoles().stream()
                    .anyMatch(r -> "ESTUDIANTE".equals(r.getName()));
            if (!esEstudiante) {
                throw new IllegalArgumentException(
                        "La persona tiene una cuenta con otro rol: no se le puede crear una ficha de estudiante");
            }
        });
    }

    /**
     * Crea el {@code UserAccount} (rol {@code ESTUDIANTE}) sobre una persona que
     * ya existe; no lo asocia a la ficha de {@code Student} —eso lo hace
     * el llamador una vez que tiene el {@code UserAccount} guardado—.
     *
     * @param persona persona dueña de la cuenta
     * @param request credenciales de la cuenta a crear
     * @return el {@code UserAccount} recién guardado
     * @throws IllegalArgumentException si el {@code username} ya está en uso
     * @throws IllegalStateException    si falta el rol {@code ESTUDIANTE} o el
     *                                  catálogo de estados en la base
     */
    public UserAccount createStudentAccount(Person persona, EnableAccessRequest request) {
        if (usuarioRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese usuario");
        }
        passwordPolicy.validate(request.password(), request.username());

        Role rolEstudiante = rolRepository.findByName("ESTUDIANTE")
                .orElseThrow(() -> new IllegalStateException("Falta el rol ESTUDIANTE (ver db/seed.sql)"));
        GeneralStatus estadoActivo = estadoGeneralRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el catalogo seguridad.estados_general (ver db/seed.sql)"));

        UserAccount usuario = UserAccount.builder()
                .person(persona)
                .generalStatus(estadoActivo)
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .active(true)
                .roles(Set.of(rolEstudiante))
                .build();
        return usuarioRepository.save(usuario);
    }
}
