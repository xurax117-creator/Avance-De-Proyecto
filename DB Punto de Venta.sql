CREATE DATABASE DB_Punto_De_Venta;
USE DB_Punto_De_Venta;

-- USUARIOS
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    alias VARCHAR(50) NOT NULL UNIQUE,
    contraseña VARCHAR(255) NOT NULL,
    rol ENUM('Administrador', 'Gerente', 'Cajero') NOT NULL,
    activo BOOLEAN DEFAULT TRUE
);

-- PRODUCTOS
CREATE TABLE productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    codigo_barras VARCHAR(50) UNIQUE,
    codigo_barras_secundario VARCHAR(50),
    nombre VARCHAR(100) NOT NULL,
    categoria VARCHAR(50),
    precio_compra DECIMAL(10,2) NOT NULL,
    precio_venta DECIMAL(10,2) NOT NULL,
    existencias_act INT DEFAULT 0,
    existencias_min INT DEFAULT 0,
    unidad_medida VARCHAR(20),
    activo BOOLEAN DEFAULT TRUE
);

-- VENTAS
CREATE TABLE ventas (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT,
    total DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- DETALLE DE VENTA
CREATE TABLE detalle_venta (
    id_detalle INT AUTO_INCREMENT PRIMARY KEY,
    id_venta INT,
    id_producto INT,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) AS (cantidad * precio_unitario) STORED,
    FOREIGN KEY (id_venta) REFERENCES ventas(id_venta),
    FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

insert into usuarios (nombre_completo, alias, contraseña, rol, activo)
values ('Axel Uriel Moreno Cervantes', 'Axel', 'A158910323728a.', 'Administrador', True);

insert into productos (codigo_barras, codigo_barras_secundario, nombre, categoria, precio_compra, precio_venta, existencias_act, existencias_min, unidad_medida, activo)
values ('123', '1234', 'Victoria Fresa-Kiwi', 'Refrescos', 15, 18, 10, 5, 'PZA', True);

select * from usuarios;
select * from productos;
select * from ventas;
select * from detalle_venta;

update productos
set existencias_act = 20
where id_producto = 1;

update usuarios
set contraseña = "1234"
where id_usuario = 1;

SELECT id_usuario, alias, contraseña, activo 
FROM usuarios 
WHERE alias = 'Axel';

ALTER TABLE productos ADD COLUMN proveedor VARCHAR(100) AFTER categoria;

update productos
set proveedor = "Coca Cola"
where id_producto = 1;

CREATE TABLE proveedores (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    contacto VARCHAR(100),
    telefono VARCHAR(20),
    activo BOOLEAN DEFAULT TRUE
);

INSERT INTO proveedores (nombre) VALUES ('Coca Cola'), ('Pepsi'), ('Bimbo'), ('Centra de Abastos');

ALTER TABLE productos ADD COLUMN id_proveedor INT AFTER categoria;

UPDATE productos SET id_proveedor = 1 WHERE id_producto = 1;

ALTER TABLE productos DROP COLUMN proveedor;

ALTER TABLE productos ADD CONSTRAINT fk_proveedor 
FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor);