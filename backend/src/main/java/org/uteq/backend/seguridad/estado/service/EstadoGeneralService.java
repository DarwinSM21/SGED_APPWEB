package org.uteq.backend.seguridad.estado.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.seguridad.estado.dto.EstadoGeneralResponse;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lectura del catálogo {@code seguridad.estados_general} (estados
 * administrativos como ACTIVO / INACTIVO). Catálogo estable: sin altas ni
 * bajas por API.
 */
@Service
@RequiredArgsConstructor
public class EstadoGeneralService {

    private final EstadoGeneralRepository estadoGeneralRepository;

    /**
     * Devuelve todos los estados del catálogo.
     *
     * @return la lista completa, mapeada a {@link EstadoGeneralResponse}
     */
    @Transactional(readOnly = true)
    public List<EstadoGeneralResponse> listarTodos() {
        return estadoGeneralRepository.findAll()
                .stream()
                .map(estado -> new EstadoGeneralResponse(
                        estado.getIdEstadoGeneral(),
                        estado.getNombre()
                ))
                .collect(Collectors.toList());
    }
}