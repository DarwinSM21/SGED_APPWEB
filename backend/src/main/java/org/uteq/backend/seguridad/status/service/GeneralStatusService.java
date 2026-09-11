package org.uteq.backend.seguridad.status.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.seguridad.status.dto.GeneralStatusResponse;
import org.uteq.backend.seguridad.status.repository.GeneralStatusRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lectura del catálogo {@code seguridad.estados_general} (estados
 * administrativos como ACTIVO / INACTIVO). Catálogo estable: sin altas ni
 * bajas por API.
 */
@Service
@RequiredArgsConstructor
public class GeneralStatusService {

    private final GeneralStatusRepository estadoGeneralRepository;

    /**
     * Devuelve todos los estados del catálogo.
     *
     * @return la lista completa, mapeada a {@link GeneralStatusResponse}
     */
    @Transactional(readOnly = true)
    public List<GeneralStatusResponse> findAll() {
        return estadoGeneralRepository.findAll()
                .stream()
                .map(estado -> new GeneralStatusResponse(
                        estado.getId(),
                        estado.getName()
                ))
                .collect(Collectors.toList());
    }
}
