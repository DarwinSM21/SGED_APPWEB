-- ============================================================
-- V8: Quinto criterio de evaluacion - Velocidad
--
-- La condicion fisica original incluia "velocidad" solo como parte de
-- su descripcion textual ("Resistencia, velocidad y fuerza"), sin ser
-- un criterio calificable por separado. Se promueve a criterio propio:
-- el entrenador evalua explosividad y rapidez con su propio slider en
-- vez de que quede diluido dentro de un puntaje mas general.
--
-- No requiere cambios de codigo: EvaluacionDiariaService itera sobre
-- criterios_evaluacion.activo, asi que una fila nueva aqui aparece
-- automaticamente en la pantalla de evaluacion y en el detalle enviado
-- a la IA (PerfilJugadorAnonimo.puntajes es un mapa por nombre).
-- ============================================================

INSERT INTO deportivo.criterios_evaluacion (nombre, descripcion, puntaje_maximo) VALUES
    ('Velocidad', 'Explosividad y rapidez en el terreno de juego', 10)
ON CONFLICT (nombre) DO NOTHING;
