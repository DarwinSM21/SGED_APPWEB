package org.uteq.backend.deportivo.match.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.ResourceNotFoundException;
import org.uteq.backend.deportivo.category.entity.Category;
import org.uteq.backend.deportivo.category.repository.CategoryRepository;
import org.uteq.backend.deportivo.evaluation.repository.LineupRepository;
import org.uteq.backend.deportivo.match.dto.MatchDtos.CreateMatchRequest;
import org.uteq.backend.deportivo.match.dto.MatchDtos.MatchPageResponse;
import org.uteq.backend.deportivo.match.dto.MatchDtos.MatchResponse;
import org.uteq.backend.deportivo.match.dto.MatchDtos.ResultRequest;
import org.uteq.backend.deportivo.match.entity.Match;
import org.uteq.backend.deportivo.match.repository.MatchRepository;
import org.uteq.backend.seguridad.audit.aop.Audited;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Instant;
import org.springframework.security.core.context.SecurityContextHolder;
import org.uteq.backend.seguridad.user.repository.UserAccountRepository;

/**
 * Alta, agenda e historial de partidos. Un partido con resultado queda
 * cerrado y solo se puede editar tras reabrirlo; el resultado ({@code
 * GANADO} / {@code EMPATADO} / {@code PERDIDO}) se calcula, no se guarda.
 */
@Service
@RequiredArgsConstructor
public class MatchService {
    private final MatchRepository matchRepository;
    private final CategoryRepository categoryRepository;
    private final LineupRepository lineupRepository;
    private final UserAccountRepository usuarioRepository;

    /**
     * Lista paginada de partidos, opcionalmente filtrada por categoría,
     * anotando cada uno con si tiene alineación y cuántos titulares.
     *
     * @param idCategoria categoría por la que filtrar, o {@code null} para
     *                    todas
     * @param pagina      número de página (se acota a {@code >= 0})
     * @param tamano      tamaño de página (se acota a {@code [1, 100]})
     * @return la página de partidos
     */
    @Transactional(readOnly = true)
    public MatchPageResponse list(Long idCategoria, int pagina, int tamano) {
        var pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100));
        Page<Match> page = idCategoria == null
                ? matchRepository.findAllOrderByDateDescTimeDesc(pageable)
                : matchRepository.findByCategoryOrderByDateDescTimeDesc(idCategoria, pageable);

        List<MatchResponse> contenido = withLineup(page.getContent());
        return new MatchPageResponse(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    /**
     * Detalle de un partido.
     *
     * @param idPartido identificador del partido
     * @return el partido, con su estado de alineación y resultado
     * @throws ResourceNotFoundException si no existe
     */
    @Transactional(readOnly = true)
    public MatchResponse findById(Long idPartido) {
        Match p = matchRepository.findWithCategoryById(idPartido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el partido " + idPartido));
        return withLineup(List.of(p)).get(0);
    }

    /**
     * Agenda un partido para una categoría activa.
     *
     * @param request categoría, fecha, hora y observación
     * @return el partido creado
     * @throws ResourceNotFoundException si la categoría no existe
     * @throws IllegalArgumentException     si la categoría está inactiva
     */
    @Audited(action = "CREAR", entity = "Match", idSpel = "#result.idPartido",
            descriptionSpel = "'agendó un partido de ' + #result.categoria + ' para el ' + #result.fecha")
    @Transactional
    public MatchResponse create(CreateMatchRequest request) {
        Category categoria = categoryRepository.findById(request.idCategoria())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la categoría " + request.idCategoria()));
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new IllegalArgumentException(
                    "La categoría " + categoria.getNombre() + " está inactiva");
        }

        Match guardado = matchRepository.save(Match.builder()
                .categoria(categoria)
                .fecha(request.fecha())
                .hora(request.hora())
                .observacion(request.observacion())
                .cerrado(false)
                .build());
        return toResponse(guardado, false, 0);
    }

    /**
     * Carga el marcador de un partido y lo cierra. El marcador llega después
     * de jugar, por eso va en su propia operación y no en la creación.
     *
     * @param idPartido identificador del partido
     * @param request   goles a favor, en contra y observación opcional
     * @return el partido cerrado
     * @throws ResourceNotFoundException si el partido no existe
     * @throws IllegalArgumentException     si el partido ya estaba cerrado
     */
    @Audited(action = "EDITAR", entity = "Match", idSpel = "#p0",
            descriptionSpel = "'cargó el resultado del partido ' + #p0 + ' y lo cerró'")
    @Transactional
    public MatchResponse registerResult(Long idPartido, ResultRequest request) {
        Match p = matchRepository.findWithCategoryById(idPartido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el partido " + idPartido));

        requireOpen(p);

        p.setGolesFavor(request.golesFavor());
        p.setGolesContra(request.golesContra());
        if (request.observacion() != null) {
            p.setObservacion(request.observacion());
        }
        p.setCerrado(true);
        p.setCerradoEn(Instant.now());
        p.setCerradoPorIdUsuario(currentUserId());
        matchRepository.save(p);
        return findById(idPartido);
    }

    /**
     * Reabre un partido cerrado para corregir el resultado o la alineación.
     *
     * @param idPartido identificador del partido
     * @return el partido reabierto
     * @throws ResourceNotFoundException si el partido no existe
     * @throws IllegalArgumentException     si el partido no estaba cerrado
     */
    @Audited(action = "EDITAR", entity = "Match", idSpel = "#p0",
            descriptionSpel = "'reabrio el partido ' + #p0 + ' para corregirlo'")
    @Transactional
    public MatchResponse reopen(Long idPartido) {
        Match p = matchRepository.findWithCategoryById(idPartido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el partido " + idPartido));

        if (!p.isClosed()) {
            throw new IllegalArgumentException("Este partido no está cerrado");
        }

        p.setCerrado(false);
        p.setCerradoEn(null);
        p.setCerradoPorIdUsuario(null);
        matchRepository.save(p);
        return findById(idPartido);
    }

    /**
     * Verifica que un partido esté abierto (no cerrado por resultado).
     *
     * @param p partido a comprobar
     * @throws IllegalArgumentException si el partido está cerrado
     */
    public void requireOpen(Match p) {
        if (p.isClosed()) {
            throw new IllegalArgumentException(
                    "El partido está cerrado. Para corregirlo, reabrilo primero.");
        }
    }

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        return usuarioRepository.findByUsernameIgnoreCaseAndActiveTrue(auth.getName())
                .map(u -> u.getId())
                .orElse(null);
    }

    /**
     * Elimina un partido. La alineación cae con él ({@code ON DELETE
     * CASCADE}): sin partido no significa nada.
     *
     * @param idPartido identificador del partido
     * @throws ResourceNotFoundException si no existe
     * @throws IllegalArgumentException     si el partido está cerrado
     */
    @Audited(action = "ELIMINAR", entity = "Match", idSpel = "#p0",
            descriptionSpel = "'eliminó el partido ' + #p0")
    @Transactional
    public void delete(Long idPartido) {
        Match p = matchRepository.findById(idPartido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el partido " + idPartido));

        requireOpen(p);

        matchRepository.delete(p);
    }

    // Anota cada partido con si tiene alineación y cuántos titulares, en dos
    // consultas para toda la página en vez de dos por fila.
    private List<MatchResponse> withLineup(List<Match> partidos) {
        if (partidos.isEmpty()) {
            return List.of();
        }
        List<Long> ids = partidos.stream().map(Match::getIdPartido).toList();
        Map<Long, Integer> titulares = new HashMap<>();
        Set<Long> conAlineacion = new HashSet<>();
        for (Object[] fila : lineupRepository.countStartersByMatch(ids)) {
            Long id = (Long) fila[0];
            conAlineacion.add(id);
            titulares.put(id, fila[1] == null ? 0 : ((Number) fila[1]).intValue());
        }
        return partidos.stream()
                .map(p -> toResponse(p, conAlineacion.contains(p.getIdPartido()),
                        titulares.getOrDefault(p.getIdPartido(), 0)))
                .toList();
    }

    private MatchResponse toResponse(Match p, boolean tieneAlineacion, int titulares) {
        return new MatchResponse(
                p.getIdPartido(),
                p.getCategoria().getIdCategoria(),
                p.getCategoria().getNombre(),
                p.getFecha(), p.getHora(),
                p.getGolesFavor(), p.getGolesContra(), p.getObservacion(),
                resultOf(p), tieneAlineacion, titulares,
                p.isClosed(), p.getCerradoEn());
    }

    // Se calcula, no se guarda: almacenar "GANADO" junto al marcador abre la
    // puerta a que un día digan cosas distintas.
    private String resultOf(Match p) {
        if (!p.hasResult()) {
            return "PENDIENTE";
        }
        int diferencia = p.getGolesFavor() - p.getGolesContra();
        if (diferencia > 0) {
            return "GANADO";
        }
        return diferencia == 0 ? "EMPATADO" : "PERDIDO";
    }
}
