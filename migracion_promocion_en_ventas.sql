-- Migración: guardar en cada línea de la venta la promoción que se le aplicó (para Reportes).
-- IMPORTANTE: ejecutar ANTES de subir el código nuevo. El código nuevo escribe en estas columnas al cobrar;
-- el código actual las ignora, así que se puede correr en cualquier momento.
ALTER TABLE detalle_venta
    ADD COLUMN promocion VARCHAR(100) NULL,
    ADD COLUMN tipo_promocion VARCHAR(20) NULL;
