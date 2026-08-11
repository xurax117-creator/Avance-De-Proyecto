-- Migración: módulo de Gastos (hoja diaria estilo Excel)
-- Ejecutar contra la base de datos del entorno local primero para probar.

-- ── Si esta es la PRIMERA VEZ que corres esta migración ──
-- (todavía no existe la tabla gastos_dia), usa este CREATE TABLE completo:

CREATE TABLE gastos_dia (
    id_gasto INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    id_sucursal INT NOT NULL,
    filas JSON NOT NULL,
    monedas DECIMAL(10,2) NOT NULL DEFAULT 0,
    billetes DECIMAL(10,2) NOT NULL DEFAULT 0,
    terminal1 DECIMAL(10,2) NOT NULL DEFAULT 0,
    terminal2 DECIMAL(10,2) NOT NULL DEFAULT 0,
    id_usuario INT NULL,
    fecha_actualizacion DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_gastos_fecha_sucursal (fecha, id_sucursal),
    CONSTRAINT fk_gastos_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- ── Si YA habías corrido la versión anterior de este script ──
-- (la tabla gastos_dia ya existe sin terminal1/terminal2), corre esto en su lugar:
--
-- ALTER TABLE gastos_dia
--     ADD COLUMN terminal1 DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER billetes,
--     ADD COLUMN terminal2 DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER terminal1;
