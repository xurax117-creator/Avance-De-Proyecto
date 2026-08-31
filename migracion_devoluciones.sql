-- Migración: módulo de Devoluciones de Ventas
-- Ejecutar contra la base de datos del entorno local primero para probar.

-- Acumulado de lo devuelto por venta (para reportar el total neto sin perder el original)
ALTER TABLE ventas ADD COLUMN total_devuelto DECIMAL(10,2) NOT NULL DEFAULT 0;

-- Encabezado de cada devolución realizada
CREATE TABLE devoluciones (
    id_devolucion INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT NOT NULL,
    id_usuario INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    total_devuelto DECIMAL(10,2) NOT NULL DEFAULT 0,
    id_sucursal INT NOT NULL,
    CONSTRAINT fk_devolucion_venta FOREIGN KEY (id_venta) REFERENCES ventas(id_venta),
    CONSTRAINT fk_devolucion_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- Detalle de qué se devolvió, ligado al renglón exacto de la venta original
-- (id_detalle_venta, no solo id_producto, para no confundir renglones del mismo
-- producto vendidos a distinto precio por una promoción)
CREATE TABLE detalle_devolucion (
    id_detalle_devolucion INT AUTO_INCREMENT PRIMARY KEY,
    id_devolucion INT NOT NULL,
    id_detalle_venta INT NOT NULL,
    id_producto INT NOT NULL,
    cantidad DECIMAL(10,3) NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detdev_devolucion FOREIGN KEY (id_devolucion) REFERENCES devoluciones(id_devolucion),
    CONSTRAINT fk_detdev_detalle_venta FOREIGN KEY (id_detalle_venta) REFERENCES detalle_venta(id_detalle),
    CONSTRAINT fk_detdev_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);
