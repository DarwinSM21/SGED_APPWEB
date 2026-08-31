package org.uteq.backend.deportivo.partido.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;
import org.uteq.backend.deportivo.categoria.entity.Categoria;
import org.uteq.backend.deportivo.categoria.repository.CategoriaRepository;
import org.uteq.backend.deportivo.evaluacion.repository.AlineacionRepository;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.CrearPartidoRequest;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.PartidoPageResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.PartidoResponse;
import org.uteq.backend.deportivo.partido.dto.PartidoDtos.ResultadoRequest;
import org.uteq.backend.deportivo.partido.entity.Partido;
import org.uteq.backend.deportivo.partido.repository.PartidoRepository;
import org.uteq.backend.seguridad.auditoria.aop.Auditado;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PartidoService {
    private final PartidoRepository partidoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AlineacionRepository alineacionRepository;

    @Transactional(readOnly = true)
    public PartidoPageResponse listar(Long idCategoria, int pagina, int tamano) {
        var pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100));
        Page<Partido> page = idCategoria == null
                ? partidoRepository.findAllByOrderByFechaDescHoraDesc(pageable)
                : partidoRepository.findByCategoria_IdCategoriaOrderByFechaDescHoraDesc(idCategoria, pageable);

        List<PartidoResponse> contenido = conAlineacion(page.getContent());
        return new PartidoPageResponse(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PartidoResponse buscarPorId(Long idPartido) {
        Partido p = partidoRepository.findWithCategoriaByIdPartido(idPartido)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el partido " + idPartido));
        return conAlineacion(List.of(p)).get(0);
    }

    @Auditado(accion = "CREAR", entidad = "Partido", idSpel = "#result.idPartido",
            descripcionSpel = "'agendó un partido de ' + #result.categoria + ' para el ' + #result.fecha")
    @Transactional
    public PartidoResponse crear(CrearPartidoRequest request) {
        Categoria categoria = categoriaRepository.findById(request.idCategoria())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoría " + request.idCategoria()));
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new IllegalArgumentException(
                    "La categoría " + categoria.getNombre() + " está inactiva");
        }

        Partido guardado = partidoRepository.save(Partido.builder()
                .categoria(categoria)
                .fecha(request.fecha())
                .hora(request.hora())
                .observacion(request.observacion())
                .build());
        return aResponse(guardado, false, 0);
    }

    @Auditado(accion = "EDITAR", entidad = "Partido", idSpel = "#p0",
            descripcionSpel = "'cargó el resultado del partido ' + #p0")
    @Transactional
    public PartidoResponse registrarResultado(Long idPartido, ResultadoRequest request) {
        Partido p = partidoRepository.findWithCategoriaByIdPartido(idPartido)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el partido " + idPartido));
        p.setGolesFavor(request.golesFavor());
        p.setGolesContra(request.golesContra());
        if (request.observacion() != null) {
            p.setObservacion(request.observacion());
        }
        partidoRepository.save(p);
        return buscarPorId(idPartido);
    }

    @Auditado(accion = "ELIMINAR", entidad = "Partido", idSpel = "#p0",
            descripcionSpel = "'eliminó el partido ' + #p0")
    @Transactional
    public void eliminar(Long idPartido) {
        Partido p = partidoRepository.findById(idPartido)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el partido " + idPartido));

        partidoRepository.delete(p);
    }

    private List<PartidoResponse> conAlineacion(List<Partido> partidos) {
        if (partidos.isEmpty()) {
            return List.of();
        }
        List<Long> ids = partidos.stream().map(Partido::getIdPartido).toList();
        Map<Long, Integer> titulares = new HashMap<>();
        Set<Long> conAlineacion = new HashSet<>();
        for (Object[] fila : alineacionRepository.contarTitularesPorPartido(ids)) {
            Long id = (Long) fila[0];
            conAlineacion.add(id);
            titulares.put(id, fila[1] == null ? 0 : ((Number) fila[1]).intValue());
        }
        return partidos.stream()
                .map(p -> aResponse(p, conAlineacion.contains(p.getIdPartido()),
                        titulares.getOrDefault(p.getIdPartido(), 0)))
                .toList();
    }

    private PartidoResponse aResponse(Partido p, boolean tieneAlineacion, int titulares) {
        return new PartidoResponse(
                p.getIdPartido(),
                p.getCategoria().getIdCategoria(),
                p.getCategoria().getNombre(),
                p.getFecha(), p.getHora(),
                p.getGolesFavor(), p.getGolesContra(), p.getObservacion(),
                resultadoDe(p), tieneAlineacion, titulares);
    }

    private String resultadoDe(Partido p) {
        if (!p.tieneResultado()) {
            return "PENDIENTE";
        }
        int diferencia = p.getGolesFavor() - p.getGolesContra();
        if (diferencia > 0) {
            return "GANADO";
        }
        return diferencia == 0 ? "EMPATADO" : "PERDIDO";
    }
}
