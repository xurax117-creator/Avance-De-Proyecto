# MRKdito POS - Sistema de Punto de Venta

## 📋 Descripción General

MRKdito POS es un sistema de punto de venta desarrollado con Spring Boot que permite gestionar las operaciones básicas de un comercio: autenticación de usuarios, gestión de inventario, procesamiento de ventas y generación de reportes.

---

## 🛠 Tecnologías Utilizadas

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| **Java** | 17 | Lenguaje de programación principal |
| **Spring Boot** | 4.0.0 | Framework backend |
| **MySQL** | 8.0+ | Base de datos relacional |
| **Maven** | - | Gestión de dependencias |
| **HTML/CSS/JavaScript** | - | Frontend estático |
| **Lombok** | - | Reducción de código boilerplate |

---

## 📁 Estructura del Proyecto

```
sistema-punto-venta-WEB/
├── pom.xml                          # Configuración de Maven
├── src/main/
│   ├── java/com/uriel/pos/sistema_punto_venta/
│   │   ├── SistemaPuntoVentaApplication.java  # Clase principal
│   │   ├── Conexion.java                      # Gestión de conexión a BD
│   │   ├── Login.java                         # Lógica de autenticación
│   │   ├── LoginController.java               # API REST de login
│   │   ├── Venta.java                         # Lógica de ventas
│   │   ├── VentaController.java               # API REST de ventas
│   │   ├── InventarioController.java          # API REST de inventario
│   │   ├── ReporteController.java             # API REST de reportes
│   │   └── ReporteDAO.java                    # Consultas SQL para reportes
│   └── resources/
│       ├── application.properties             # Configuración de BD
│       └── static/
│           ├── login.html                     # Pantalla de inicio de sesión
│           ├── menu.html                      # Menú principal
│           ├── inventario.html                # Gestión de productos
│           ├── venta.html                     # Terminal de punto de venta
│           └── reportes.html                  # Panel de reportes
```

---

## 🗄 Base de Datos

### 📊 Esquema de Tablas (Inferido del Código)

```sql
-- 1. TABLA DE USUARIOS
CREATE TABLE usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    alias VARCHAR(50) NOT NULL UNIQUE,
    contraseña VARCHAR(255) NOT NULL,
    rol ENUM('Administrador', 'Gerente', 'Cajero') NOT NULL,
    activo BOOLEAN DEFAULT TRUE
);

-- 2. TABLA DE PROVEEDORES
CREATE TABLE proveedores (
    id_proveedor INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    contacto VARCHAR(100),
    telefono VARCHAR(20),
    activo BOOLEAN DEFAULT TRUE
);

-- 3. TABLA DE PRODUCTOS
CREATE TABLE productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    codigo_barras VARCHAR(50) UNIQUE,
    codigo_barras_secundario VARCHAR(50),
    nombre VARCHAR(100) NOT NULL,
    categoria VARCHAR(50),
    id_proveedor INT, -- Relación con proveedores
    precio_compra DECIMAL(10,2) NOT NULL,
    precio_venta DECIMAL(10,2) NOT NULL,
    existencias_act INT DEFAULT 0,
    existencias_min INT DEFAULT 0,
    unidad_medida VARCHAR(20),
    activo BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (id_proveedor) REFERENCES proveedores(id_proveedor)
);

-- 4. TABLA DE VENTAS
CREATE TABLE ventas (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_usuario INT,
    total DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- 5. TABLA DE DETALLE DE VENTA
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
```

### ⚙️ Configuración de Conexión

```properties
# application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/DB_Punto_De_Venta
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

> **Nota:** Actualizar `password` según la configuración de tu servidor MySQL local.

---

## 🔐 Módulo de Autenticación

### Descripción
El sistema de autenticación permite a los usuarios acceder al sistema mediante un alias y contraseña. Los usuarios activos pueden iniciar sesión y sus datos se almacenan en `localStorage` del navegador.

### Flujo de Autenticación
```
1. Usuario ingresa alias y contraseña
2. Frontend envía credenciales al endpoint /api/login
3. Backend valida contra tabla usuarios (activo = TRUE)
4. Si es válido, devuelve: userId, nombre, rol
5. Frontend almacena datos en localStorage
6. Redirección al menú principal
```

### 📍 Endpoint API

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/login` | Autenticar usuario |

**Request:**
```json
{
  "alias": "admin",
  "pass": "contraseña123"
}
```

**Response (Éxito):**
```json
{
  "success": true,
  "message": "Acceso concedido",
  "userId": 1,
  "nombre": "Nombre del Usuario",
  "rol": "admin"
}
```

**Response (Error):**
```json
{
  "success": false,
  "message": "Usuario o contraseña incorrectos, o cuenta inactiva."
}
```

### 📷 Pantallas

> ![Login MRKdito POS](/src/Images/LoginMRKdito.png)
> 
> *Descripción: Pantalla de inicio de sesión con logo MRKdito, campos de usuario y contraseña, y botón de acceso.*

---

## 📦 Módulo de Inventario

### Descripción
Permite gestionar el catálogo de productos incluyendo: creación, edición, búsqueda y filtrado por nivel de stock. También permite administrar proveedores.

### Funcionalidades
- ✅ Listar todos los productos activos
- ✅ Crear nuevos productos
- ✅ Editar productos existentes (doble clic en la tabla)
- ✅ Buscar por nombre o código de barras
- ✅ Filtrar productos con stock bajo el mínimo
- ✅ Gestión de proveedores
- ✅ Eliminación lógica (activo = FALSE)

### 📍 Endpoints API

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/inventario/todos` | Listar todos los productos |
| `POST` | `/api/inventario/guardar` | Crear/Actualizar producto |
| `GET` | `/api/inventario/proveedores` | Listar proveedores |

#### GET /api/inventario/todos
**Response:**
```json
[
  {
    "id_producto": 1,
    "codigo_barras": "7501001234567",
    "nombre": "Producto Ejemplo",
    "categoria": "Refrescos",
    "nombre_proveedor": "Proveedor ABC",
    "id_proveedor": 1,
    "precio_compra": 10.00,
    "precio_venta": 15.00,
    "existencias_act": 50,
    "existencias_min": 10
  }
]
```

#### POST /api/inventario/guardar
**Request:**
```json
{
  "codigo_barras": "7501001234567",
  "nombre": "Nuevo Producto",
  "categoria": "Snacks",
  "id_proveedor": "1",
  "precio_compra": 8.50,
  "precio_venta": 12.00,
  "existencias_act": 100,
  "existencias_min": 20
}
```

**Response:**
```json
{
  "success": true
}
```

### 📷 Pantallas

> ![Inventario MRKdito POS](/src/Images/InventarioMRKdito.png)
> 
> *Descripción: Tabla con lista de productos, filtros de búsqueda y botón para nuevo producto.*

> ![Agregar Producto MRKdito POS](/src/Images/InventarioAgregarProductoMRKdito.png)
> 
> *Descripción: Formulario modal para crear/editar producto con todos los campos.*

---

## 🛒 Módulo de Ventas (Punto de Venta)

### Descripción
Terminal de punto de venta para procesar transacciones comerciales. Incluye escaneo de códigos de barras, gestión de carrito de compras y cobro de ventas.

### Flujo de Venta
```
1. Cajero escanea código de barras o lo escribe manualmente
2. Sistema busca producto y lo agrega al carrito
3. Se puede modificar cantidad o eliminar productos
4. Al finalizar, se procesa el cobro
5. Sistema:
   - Crea registro en tabla ventas
   - Inserta detalles en tabla detalle_venta
   - Actualiza stock en tabla productos
6. Pantalla se limpia para siguiente venta
```

### 📍 Endpoints API

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/api/venta/producto` | Buscar y agregar producto |
| `POST` | `/api/venta/finalizar` | Finalizar transacción |

#### POST /api/venta/producto
**Request:**
```json
{
  "codigo": "7501001234567",
  "idVentaActual": -1,
  "userId": 1
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "idProducto": 1,
    "nombre": "Producto Ejemplo",
    "precio": 15.00,
    "stock": 50,
    "idVenta": 1
  }
}
```

#### POST /api/venta/finalizar
**Request:**
```json
{
  "userId": 1,
  "idVenta": 1,
  "totalFinal": 150.00,
  "carrito": [
    {
      "idProducto": 1,
      "cantidad": 5,
      "precioUnitario": 15.00
    },
    {
      "idProducto": 2,
      "cantidad": 3,
      "precioUnitario": 25.00
    }
  ]
}
```

### 📷 Pantallas

> ![Ventas MRKdito POS](/src/Images/VentasMRKdito.png)
> 
> *Descripción: Terminal de venta con campo de escaneo, tabla del carrito, total y botones de cobrar/cancelar.*

> ![Ventas Carrito MRKdito POS](/src/Images/VentasCarritoLLenoMRKdito.png)
> 
> *Descripción: Pantalla mostrando productos agregados al carrito con cantidades y subtotales.*

---

## 📈 Módulo de Reportes

### Descripción
Panel de reportes con estadísticas de ventas. Permite visualizar datos por período y exportar información detallada.

### Funcionalidades
- 📊 Ventas diarias con total acumulado
- 🏆 Top 10 productos más vendidos
- 👤 Ventas por cajero/usuario
- 🧾 Detalle de tickets de venta (modal)

### 📍 Endpoints API

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/api/reportes/ventas` | Lista de ventas por período |
| `GET` | `/api/reportes/detalle/{id}` | Detalle de una venta |
| `GET` | `/api/reportes/productos` | Top productos vendidos |
| `GET` | `/api/reportes/cajeros` | Ventas por cajero |

#### GET /api/reportes/ventas?inicio=2024-01-01&fin=2024-01-31
**Response:**
```json
[
  {
    "idVenta": 1,
    "fecha": "2024-01-15 14:30:00",
    "nombre": "Juan Pérez",
    "total": 250.00
  }
]
```

#### GET /api/reportes/productos?inicio=2024-01-01&fin=2024-01-31
**Response:**
```json
[
  {
    "nombre": "Refresco Cola 600ml",
    "cantidad": 150,
    "total": 2250.00
  }
]
```

#### GET /api/reportes/cajeros?inicio=2024-01-01&fin=2024-01-31
**Response:**
```json
[
  {
    "nombre": "Juan Pérez",
    "numVentas": 25,
    "total": 5250.00
  }
]
```

### 📷 Pantallas

> ![Reporte Ventas Diarias](/src/Images/ReporteVentasDiarias.png)
> 
> *Descripción: Panel con filtros de fecha, tabla de ventas y resumen total del período.*

> ![Reporte Top Productos MRKdito POS](/src/Images/ReporteTopProductos.png)
> 
> *Descripción: Reporte de productos más vendidos con cantidades y totales.*

> ![Reporte Ventas por Cajero MRKdito POS](/src\Images\ReporteVentasPorCajero.png)
> 
> *Descripción: Estadísticas de ventas por usuario con número de transacciones y monto.*

> ![Detalle de la Venta MRKdito POS](/src\Images\TicketDetalleDeVenta.png)
> 
> *Descripción: Visualización estilo ticket de venta con desglose de productos.*

---

## 📱 Menú Principal

### Descripción
Pantalla de navegación que permite acceder a los diferentes módulos del sistema según el rol del usuario autenticado.

### 📷 Pantalla

> ![Menú Principal MRKdito POS](/src\Images\MenuPrincipalMRKdito.png)
> 
> *Descripción: Pantalla de bienvenida con tarjetas de navegación a Ventas, Inventario, Reportes y botón de cierre de sesión.*

---

## 🚀 Instalación y Ejecución

### Prerrequisitos
- JDK 17 o superior
- Maven 3.6+
- MySQL Server 8.0+
- IDE recomendado: VS Code o IntelliJ IDEA

### Pasos de Instalación

1. **Clonar o descargar el proyecto**
   ```bash
   git clone <url-del-repositorio>
   cd sistema-punto-venta-WEB
   ```

2. **Crear la base de datos**
   ```sql
   CREATE DATABASE DB_Punto_De_Venta CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Ejecutar el script SQL de esquema**
   ```bash
   mysql -u root -p DB_Punto_De_Venta < esquema.sql
   ```

4. **Configurar credenciales de BD**
   - Editar `src/main/resources/application.properties`
   - Actualizar `spring.datasource.password` con tu contraseña de MySQL

5. **Compilar y ejecutar**
   ```bash
   # Usando Maven Wrapper (Windows)
   .\mvnw spring-boot:run
   
   # O usando Maven directo
   mvn spring-boot:run
   ```

6. **Acceder a la aplicación**
   - Abrir navegador en: `http://localhost:8080`
   - Pantalla de login: `http://localhost:8080/login.html`

---

## 👥 Roles de Usuario

| Rol | Permisos |
|-----|----------|
| **admin** | Acceso total a todos los módulos |
| **cajero** | Acceso a Ventas y Reportes (solo lectura) |
| **inventario** | Acceso a Inventario y Reportes |

---

## 🔧 Mantenimiento y Expansión

### Mejoras Sugeridas

1. **Seguridad**
   - Implementar encriptación de contraseñas (BCrypt)
   - Agregar JWT para autenticación
   - Implementar registro de auditoría

2. **Inventario**
   - Alertas de stock bajo por email
   - Historial de cambios de precios
   - Código de barras secundario

3. **Ventas**
   - Descuentos y promociones
   - Métodos de pago múltiples
   - Notas y observaciones por venta

4. **Reportes**
   - Exportación a Excel/PDF
   - Gráficos y estadísticas visuales
   - Envío automático de reportes por email

5. **General**
   - Diseño responsivo para móviles
   - Multi-sucursal
   - Respaldo automático de BD

---

## 📄 Licencia

Este proyecto fue desarrollado para uso educativo y comercial. Todos los derechos reservados.

---

## 📞 Informacion

**Desarrolladores:** 
- **Axel Uriel Moreno Cervantes**
- **Carlos Mendoza Gonzalez**
- **Valeria Jimenez Niebla**


---

