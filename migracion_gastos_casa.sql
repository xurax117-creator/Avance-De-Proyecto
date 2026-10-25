-- Migración: Gastos Casa como lista de cantidades en la conciliación.
-- gastos_casa sigue guardando el total; gastos_casa_detalle guarda cada cantidad (JSON, ej. [150,80.5,200]).
-- Ejecutar en local primero y luego en producción.
ALTER TABLE conciliaciones
    ADD COLUMN gastos_casa_detalle TEXT NULL AFTER gastos_casa;
