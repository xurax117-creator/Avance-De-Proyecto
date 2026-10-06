-- Migración: módulo de Conciliación (cuadre de caja diario)
-- Ejecutar contra la base de datos del entorno local primero para probar.

-- 1) Devoluciones: registrar cuánto de cada reembolso fue efectivo vs tarjeta,
--    para poder calcular la caja del día EN QUE SE HIZO la devolución
--    (y no del día de la venta original).
ALTER TABLE devoluciones
    ADD COLUMN monto_efectivo_devuelto DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN monto_tarjeta_devuelto DECIMAL(10,2) NOT NULL DEFAULT 0;

-- 2) Tabla de conciliaciones cerradas (una por fecha + sucursal).
--    Guarda tanto los valores automáticos como los manuales, ya congelados,
--    como una fila de tu Excel una vez escrita.
CREATE TABLE conciliaciones (
    id_conciliacion INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    id_sucursal INT NOT NULL,
    monto_inicial DECIMAL(10,2) NOT NULL,
    venta_efectivo DECIMAL(10,2) NOT NULL,
    venta_tarjetas_con_comision DECIMAL(10,2) NOT NULL,
    venta_tarjetas_sin_comision DECIMAL(10,2) NOT NULL,
    venta_total DECIMAL(10,2) NOT NULL,
    gastos DECIMAL(10,2) NOT NULL,
    gastos_casa DECIMAL(10,2) NOT NULL DEFAULT 0,
    personas_que_deben DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_efectivo_teorico DECIMAL(10,2) NOT NULL,
    comision_bancaria DECIMAL(10,2) NOT NULL DEFAULT 0,
    disposiciones_efectivo DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_bancos_teorico DECIMAL(10,2) NOT NULL,
    total_efectivo_real DECIMAL(10,2) NOT NULL,
    total_bancos_real DECIMAL(10,2) NOT NULL,
    diferencia_efectivo DECIMAL(10,2) NOT NULL,
    diferencia_bancos DECIMAL(10,2) NOT NULL,
    id_usuario_cierre INT NOT NULL,
    fecha_cierre DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_conciliacion_fecha_sucursal (fecha, id_sucursal),
    CONSTRAINT fk_conciliacion_usuario FOREIGN KEY (id_usuario_cierre) REFERENCES usuarios(id_usuario)
);
