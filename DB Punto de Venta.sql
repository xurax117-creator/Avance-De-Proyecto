CREATE DATABASE DB_Punto_De_Venta;
USE DB_Punto_De_Venta;

CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    alias VARCHAR(50) NOT NULL UNIQUE,
    contraseña VARCHAR(255) NOT NULL,
    rol ENUM('Administrador', 'Gerente', 'Cajero') NOT NULL,
    activo BOOLEAN DEFAULT TRUE
);

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

CREATE TABLE ventas (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT,
    total DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

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

CREATE TABLE proveedores (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    contacto VARCHAR(100),
    telefono VARCHAR(20),
    activo BOOLEAN DEFAULT TRUE
);

ALTER TABLE productos ADD COLUMN id_proveedor INT AFTER categoria;

ALTER TABLE productos ADD CONSTRAINT fk_proveedor 
FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor);

INSERT INTO usuarios (nombre_completo, alias, contraseña, rol, activo)
VALUES ('Axel Uriel Moreno Cervantes', 'Axel', '$2a$10$8K1p/a0dL1LXMIgoEDFrwOfMQsV3H', 'Administrador', True);

INSERT INTO proveedores (nombre) VALUES ('Coca Cola'), ('Pepsi'), ('Bimbo'), ('Centra de Abastos');

INSERT INTO productos (codigo_barras, codigo_barras_secundario, nombre, categoria, id_proveedor, precio_compra, precio_venta, existencias_act, existencias_min, unidad_medida, activo)
VALUES ('123', '1234', 'Victoria Fresa-Kiwi', 'Refrescos', 1, 15, 18, 10, 5, 'PZA', True);
