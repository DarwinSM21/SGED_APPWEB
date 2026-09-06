package org.uteq.backend.seguridad.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.seguridad.user.dto.UserAccountPageResponse;
import org.uteq.backend.seguridad.user.dto.UserAccountRequest;
import org.uteq.backend.seguridad.user.dto.UserAccountResponse;
import org.uteq.backend.seguridad.user.service.UserAccountService;

/**
 * CRUD de cuentas de usuario. Toda la clase está reservada a
 * {@code ADMINISTRADOR} ({@code @PreAuthorize} a nivel de tipo); el endpoint
 * "mis datos" del propio usuario vive aparte, en {@link PerfilController}.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UserAccountController {

    private final UserAccountService usuarioService;

    /**
     * Lista paginada de cuentas.
     *
     * @param pageable paginación; por defecto página 0, 10 por página
     * @return {@code 200 OK} con la página
     */
    @GetMapping
    public ResponseEntity<UserAccountPageResponse<UserAccountResponse>> list(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.list(pageable));
    }

    /**
     * Busca una cuenta activa por su identificador.
     *
     * @param id identificador de la cuenta
     * @return {@code 200 OK} con la cuenta
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe o está inactivada ({@code 404})
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserAccountResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.findById(id));
    }

    /**
     * Crea una cuenta para una persona ya registrada.
     *
     * @param request datos de la cuenta; validado con {@code @Valid}
     * @return {@code 201 Created} con la cuenta creada
     * @throws IllegalArgumentException si el {@code username} ya existe, falta
     *                                  la contraseña o el rol no es coherente
     *                                  con la ficha de la persona ({@code 422})
     */
    @PostMapping
    public ResponseEntity<UserAccountResponse> create(@Valid @RequestBody UserAccountRequest request) {
        UserAccountResponse creado = usuarioService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * Actualiza una cuenta.
     *
     * @param id      identificador de la cuenta a editar
     * @param request datos nuevos; validado con {@code @Valid}
     * @return {@code 200 OK} con la cuenta actualizada
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si la cuenta o la persona no existen ({@code 404})
     * @throws IllegalArgumentException si el {@code username} nuevo ya está
     *                                  ocupado o el rol no es coherente
     *                                  ({@code 422})
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserAccountResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserAccountRequest request) {
        return ResponseEntity.ok(usuarioService.update(id, request));
    }

    /**
     * Baja lógica de una cuenta.
     *
     * @param id identificador de la cuenta
     * @return {@code 204 No Content}
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        usuarioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactiva una cuenta dada de baja.
     *
     * @param id identificador de la cuenta
     * @return {@code 200 OK} con la cuenta reactivada
     * @throws org.uteq.backend.common.exception.ResourceNotFoundException
     *         si no existe ({@code 404})
     * @throws IllegalArgumentException si la cuenta ya está activa ({@code 422})
     */
    @PostMapping("/{id}/reactivar")
    public ResponseEntity<UserAccountResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.reactivate(id));
    }
}
