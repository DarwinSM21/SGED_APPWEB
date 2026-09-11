package org.uteq.backend.seguridad.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.uteq.backend.common.exception.ApiException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Política de contraseñas del sistema (RNF-14). Es la única fuente de la
 * regla: la aplican el registro de usuarios ({@code AuthService.register}),
 * el alta y el cambio administrativo de contraseña
 * ({@code UserAccountService}), la activación de acceso del estudiante
 * ({@code StudentAccessService}) y el restablecimiento por enlace
 * ({@code PasswordResetService}).
 *
 * <p>Reglas:
 * <ul>
 *   <li>longitud mínima de 8 caracteres;</li>
 *   <li>máximo 72 bytes UTF-8 — límite de BCrypt: más allá se trunca en
 *       silencio y dos contraseñas distintas podrían quedar equivalentes;</li>
 *   <li>al menos una letra y al menos un dígito;</li>
 *   <li>distinta del nombre de usuario, sin distinguir mayúsculas.</li>
 * </ul>
 */
@Component
public class PasswordPolicy {

    /** Longitud mínima exigida, en caracteres. */
    public static final int MIN_LONGITUD = 8;

    /** Máximo de bytes UTF-8 que BCrypt procesa sin truncar. */
    public static final int MAX_BYTES = 72;

    /**
     * Enumera los incumplimientos de la política. Una lista vacía significa
     * que la contraseña es válida.
     *
     * @param password contraseña candidata; {@code null} se trata como cadena
     *                 vacía
     * @param username nombre de usuario asociado, para la regla de igualdad;
     *                 {@code null} o en blanco omite esa regla (p. ej. en el
     *                 restablecimiento, donde el usuario se resuelve del token)
     * @return los mensajes de cada regla incumplida, en orden
     */
    public List<String> problems(String password, String username) {
        List<String> problems = new ArrayList<>();
        String p = password == null ? "" : password;

        if (p.length() < MIN_LONGITUD) {
            problems.add("La contraseña debe tener al menos " + MIN_LONGITUD + " caracteres.");
        }
        if (p.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            problems.add("La contraseña no puede superar los " + MAX_BYTES + " bytes.");
        }
        if (p.chars().noneMatch(Character::isLetter)) {
            problems.add("La contraseña debe incluir al menos una letra.");
        }
        if (p.chars().noneMatch(Character::isDigit)) {
            problems.add("La contraseña debe incluir al menos un dígito.");
        }
        if (username != null && !username.isBlank() && p.equalsIgnoreCase(username.trim())) {
            problems.add("La contraseña no puede ser igual al nombre de usuario.");
        }
        return problems;
    }

    /**
     * Valida la contraseña contra la política.
     *
     * @param password contraseña candidata
     * @param username nombre de usuario asociado, o {@code null}
     * @throws ApiException {@code 422 Unprocessable Entity}, con todos los
     *                      incumplimientos en el detalle, si la contraseña no
     *                      cumple la política
     */
    public void validate(String password, String username) {
        List<String> problems = problems(password, username);
        if (!problems.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, String.join(" ", problems));
        }
    }
}
