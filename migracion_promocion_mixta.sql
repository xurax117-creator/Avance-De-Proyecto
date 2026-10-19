-- Migración: promociones de "cantidad mixta" (ej. cartón revuelto de 24 entre Corona y Victoria).
-- Agrega el nuevo valor 'mix_and_match' al ENUM de tipo. Ejecutar en local primero y luego en producción.
ALTER TABLE promociones
    MODIFY COLUMN tipo ENUM('buy_x_get_y', 'percent_off', 'fixed_discount', 'mix_and_match') NOT NULL;
