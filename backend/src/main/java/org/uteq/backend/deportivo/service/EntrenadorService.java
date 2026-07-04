package org.uteq.backend.deportivo.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.auth.entity.Persona;
import org.uteq.backend.auth.repository.PersonaRepository;
import org.uteq.backend.deportivo.dto.EntrenadorRequest;
import org.uteq.backend.deportivo.dto.EntrenadorResponse;
import org.uteq.backend.deportivo.entity.Entrenador;
import org.uteq.backend.deportivo.repository.EntrenadorRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EntrenadorService {

    private final EntrenadorRepository entrenadorRepository;
    private final PersonaRepository personaRepository;

    /**
     * Crear nuevo entrenador
     */
    public EntrenadorResponse crearEntrenador(EntrenadorRequest request) {
        
        // Validar que la persona exista
        Persona persona = personaRepository.findById(request.getIdPersona())
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        
        // Validar que no exista ya un entrenador para esta persona
        if (entrenadorRepository.existsByPersonaIdPersona(request.getIdPersona())) {
            throw new IllegalArgumentException("Ya existe un entrenador para esta persona");
        }
        
        Entrenador entrenador = Entrenador.builder()
                .persona(persona)
                .especialidad(request.getEspecialidad())
                .fechaContratacion(request.getFechaContratacion())
                .activo(true)
                .build();
        
        entrenador = entrenadorRepository.save(entrenador);
        log.info("Entrenador creado: {}", entrenador.getIdEntrenador());
        return mapToResponse(entrenador);
    }

    /**
     * Obtener entrenador por ID
     */
    @Transactional(readOnly = true)
    public EntrenadorResponse obtenerEntrenador(Long idEntrenador) {
        return entrenadorRepository.findById(idEntrenador)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Entrenador no encontrado"));
    }

    /**
     * Listar entrenadores activos (paginado)
     */
    @Transactional(readOnly = true)
    public Page<EntrenadorResponse> listarEntrenadores(Pageable pageable) {
        return entrenadorRepository.findByActivoTrue(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Obtener todos los entrenadores activos (sin paginar)
     */
    @Transactional(readOnly = true)
    public List<EntrenadorResponse> obtenerTodosEntrenadores() {
        return entrenadorRepository.findByActivoTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Actualizar entrenador
     */
    public EntrenadorResponse actualizarEntrenador(Long idEntrenador, EntrenadorRequest request) {
        Entrenador entrenador = entrenadorRepository.findById(idEntrenador)
                .orElseThrow(() -> new IllegalArgumentException("Entrenador no encontrado"));
        
        if (request.getEspecialidad() != null) {
            entrenador.setEspecialidad(request.getEspecialidad());
        }
        if (request.getFechaContratacion() != null) {
            entrenador.setFechaContratacion(request.getFechaContratacion());
        }
        
        entrenador = entrenadorRepository.save(entrenador);
        log.info("Entrenador actualizado: {}", idEntrenador);
        return mapToResponse(entrenador);
    }

    /**
     * Desactivar entrenador (soft delete)
     */
    public void desactivarEntrenador(Long idEntrenador) {
        Entrenador entrenador = entrenadorRepository.findById(idEntrenador)
                .orElseThrow(() -> new IllegalArgumentException("Entrenador no encontrado"));
        
        entrenador.setActivo(false);
        entrenadorRepository.save(entrenador);
        log.info("Entrenador desactivado: {}", idEntrenador);
    }

    /**
     * Mapear a DTO de respuesta
     */
    private EntrenadorResponse mapToResponse(Entrenador entrenador) {
        return EntrenadorResponse.builder()
                .idEntrenador(entrenador.getIdEntrenador())
                .idPersona(entrenador.getPersona().getId())
                .nombreCompleto(entrenador.getPersona().getNombreCompleto())
                .email(entrenador.getPersona().getEmail())
                .especialidad(entrenador.getEspecialidad())
                .fechaContratacion(entrenador.getFechaContratacion())
                .activo(entrenador.getActivo())
                .creadoEn(LocalDateTime.ofInstant(entrenador.getCreadoEn(), ZoneId.systemDefault()))
                .build();
    }
}
