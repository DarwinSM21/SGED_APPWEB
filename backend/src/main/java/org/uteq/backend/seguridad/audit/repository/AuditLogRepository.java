package org.uteq.backend.seguridad.audit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.uteq.backend.seguridad.audit.entity.AuditLog;

/**
 * Acceso a la bitácora de auditoría. No declara consultas propias: expone
 * las operaciones CRUD de {@link JpaRepository} y el filtrado dinámico de
 * {@link JpaSpecificationExecutor} que usa la consulta paginada del panel.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
