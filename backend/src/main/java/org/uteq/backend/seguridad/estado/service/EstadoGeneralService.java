package org.uteq.backend.seguridad.estado.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.seguridad.estado.dto.EstadoGeneralResponse;
import org.uteq.backend.seguridad.estado.repository.EstadoGeneralRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstadoGeneralService {

    private final EstadoGeneralRepository estadoGeneralRepository;

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