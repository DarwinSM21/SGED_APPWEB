-- sp_reporte_stock_bajo
-- Propósito: reporte -conteo agregado de artículos activos cuyo
--            stock_actual está en o por debajo de su stock_minimo. El
--            listado detallado de esos artículos se resuelve en el
--            repositorio JPA (ArticuloRepository); este procedimiento
--            entrega el total, igual que el resto del catálogo entrega
--            un escalar OUT (no un result set).
-- Entrada:  ninguna
-- Salida:   total_bajo_stock BIGINT (parametro OUT)
-- Tablas:   inventario.articulos
-- Sin SQL dinámico. Parámetros nombrados.
--
-- Es un PROCEDURE (no FUNCTION): ver nota en
-- sp_contar_estudiantes_activos.sql sobre por que hace falta.
CREATE OR REPLACE PROCEDURE inventario.sp_reporte_stock_bajo(
    OUT total_bajo_stock BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    SELECT COUNT(*)
      INTO total_bajo_stock
      FROM inventario.articulos
     WHERE activo = TRUE
       AND stock_actual <= stock_minimo;
END;
$$;
