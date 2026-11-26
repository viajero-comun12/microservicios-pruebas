
## 📋 Requisitos Previos

- **Docker** y **Docker Compose** instalados y en ejecución:
  - En **Windows / Mac**: Tener abierto **Docker Desktop** (con estado verde *"Engine running"*).
  - En **Linux / AWS EC2**: Tener instalado el servicio `docker` y el plugin `docker-compose-v2`.

---

## 🚀 Guía de Ejecución Paso a Paso

### Paso 1: Levantar la Arquitectura

Abre una terminal en la raíz del proyecto y ejecuta:

```bash
docker compose up --build -d
```

> **¿Qué hace este comando?**
> - Construye las imágenes Docker de los 4 microservicios usando sus respectivos `Dockerfile`.
> - Crea la red virtual y los 4 volúmenes persistentes (`users-data`, `auth-data`, `orders-data`, `products-data`).
> - Inicia los 4 contenedores en segundo plano (`-d`).

#### Resultado esperado en consola:
```text
✔ Network microservicios_default       Created
✔ Volume "microservicios_users-data"    Created
✔ Volume "microservicios_auth-data"     Created
✔ Volume "microservicios_orders-data"   Created
✔ Volume "microservicios_products-data" Created
✔ Container users-api                  Started
✔ Container auth-api                   Started
✔ Container orders-api                 Started
✔ Container products-api               Started
```

---

### Paso 2: Verificar el Estado de los Contenedores

Ejecuta:
```bash
docker compose ps
```

#### Resultado esperado:
Los cuatro contenedores deben encontrarse en estado `Up` o `running` con sus puertos asignados:

```text
NAME           IMAGE                         STATUS         PORTS
users-api      microservicios-users-api      Up             0.0.0.0:8000->8000/tcp
auth-api       microservicios-auth-api       Up             0.0.0.0:8001->8001/tcp
orders-api     microservicios-orders-api     Up             0.0.0.0:8002->8002/tcp
products-api   microservicios-products-api   Up             0.0.0.0:8003->8003/tcp
```

> 💡 **Acceso directo desde el Navegador:**  
> Puedes abrir cualquiera de los puertos directamente en tu navegador (por ejemplo `http://localhost:8000` o `http://localhost:8003`) y verás un mensaje JSON de bienvenida con el estado `running` y la lista de endpoints disponibles de ese microservicio. Para ver los datos almacenados, añade la ruta correspondiente (`/users`, `/orders`, `/products`).

---

### Paso 3: Visualizar Logs de los Servicios

Para monitorear el comportamiento en tiempo real de todos los microservicios:

```bash
# Ver logs de todos los servicios combinados
docker compose logs -f

# O ver los logs de un servicio en particular
docker compose logs -f users-api
docker compose logs -f auth-api
docker compose logs -f orders-api
docker compose logs -f products-api
```

Para salir de la visualización en vivo de los logs, presiona `Ctrl + C`.

---

## 🧪 Pruebas Automáticas

El repositorio incluye dos scripts listos para probar automáticamente la creación y consulta de datos en los cuatro microservicios:

### En Windows (PowerShell):
```powershell
.\test-endpoints.ps1
```

### En Linux / Mac / AWS (Bash):
```bash
chmod +x test-endpoints.sh
./test-endpoints.sh
```

#### Resultado esperado de las pruebas:
```text
==============================================
 Probando Microservicios con SQLite y Docker  
==============================================

[1/4] Probando Users API (Python - :8000)...
Usuario creado exitosamente:  {"email":"usuario_8026@test.com","id":1,"name":"Usuario 8026"}
Total usuarios en SQLite:  1

[2/4] Probando Auth API (Java - :8001)...
Health Check:  {"port":8001,"service":"Auth API (Java)","status":"UP"}
Login exitoso:  {"valid":true,"message":"Authentication successful","status":"success","username":"admin"}

[3/4] Probando Orders API (Go - :8002)...
Pedido creado exitosamente:  {"id":1,"customer_name":"Cliente 8026","item_description":"Laptop Thinkpad","amount":950,"status":"PENDING","created_at":"2026-11-26T22:15:24Z"}
Total pedidos en SQLite:  1

[4/4] Probando Products API (JavaScript - :8003)...
Producto creado exitosamente:  {"id":1,"name":"Teclado 8026","price":49.99,"description":"Switch Red","stock":25}
Total productos en SQLite:  1

==============================================
 Todas las pruebas finalizaron con exito!    
==============================================
```

*(Cada vez que ejecutes el script, los contadores de registros en SQLite aumentarán, demostrando la persistencia de las operaciones).*

---

## 📡 Pruebas Manuales de Endpoints (cURL)

A continuación se detallan las peticiones HTTP y las respuestas JSON esperadas para cada uno de los microservicios:

---

### 1. Users API (Python - Puerto 8000)

#### 1.1 Registrar un usuario (`POST /users`)
```bash
curl -X POST http://localhost:8000/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Carlos Gomez", "email": "carlos@example.com", "password": "password123"}'
```
**Respuesta esperada (HTTP 201 Created):**
```json
{
  "id": 1,
  "name": "Carlos Gomez",
  "email": "carlos@example.com"
}
```

#### 1.2 Consultar la lista de usuarios (`GET /users`)
```bash
curl -X GET http://localhost:8000/users
```
**Respuesta esperada (HTTP 200 OK):**
```json
[
  {
    "id": 1,
    "name": "Carlos Gomez",
    "email": "carlos@example.com"
  }
]
```

#### 1.3 Consultar un usuario por ID (`GET /users/1`)
```bash
curl -X GET http://localhost:8000/users/1
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "id": 1,
  "name": "Carlos Gomez",
  "email": "carlos@example.com"
}
```

#### 1.4 Actualizar la información de un usuario (`PUT /users/1`)
```bash
curl -X PUT http://localhost:8000/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Carlos Gomez Actualizado", "email": "carlos.nuevo@example.com"}'
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "id": 1,
  "name": "Carlos Gomez Actualizado",
  "email": "carlos.nuevo@example.com"
}
```

#### 1.5 Eliminar un usuario (`DELETE /users/1`)
```bash
curl -X DELETE http://localhost:8000/users/1
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "message": "User 1 deleted successfully"
}
```

---

### 2. Auth API (Java - Puerto 8001)

> Al iniciarse, la base de datos `auth.db` se precarga automáticamente con los usuarios de prueba:
> - Usuario: `admin` | Clave: `admin123`
> - Usuario: `user1` | Clave: `password123`

#### 2.1 Verificar estado de la API (`GET /health`)
```bash
curl -X GET http://localhost:8001/health
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "status": "UP",
  "service": "Auth API (Java)",
  "port": 8001
}
```

#### 2.2 Validación de Login exitoso (`POST /login`)
```bash
curl -X POST http://localhost:8001/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "valid": true,
  "status": "success",
  "message": "Authentication successful",
  "username": "admin"
}
```

#### 2.3 Validación de Login con credenciales incorrectas (`POST /login`)
```bash
curl -X POST http://localhost:8001/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "clave_erronea"}'
```
**Respuesta esperada (HTTP 401 Unauthorized):**
```json
{
  "valid": false,
  "status": "error",
  "message": "Invalid credentials"
}
```

#### 2.4 Registrar nuevas credenciales (`POST /register`)
```bash
curl -X POST http://localhost:8001/register \
  -H "Content-Type: application/json" \
  -d '{"username": "estudiante", "password": "miclave123"}'
```
**Respuesta esperada (HTTP 201 Created):**
```json
{
  "message": "User credentials registered successfully",
  "username": "estudiante"
}
```

#### 2.5 Consultar historial de autenticaciones almacenado en SQLite (`GET /logs`)
```bash
curl -X GET http://localhost:8001/logs
```
**Respuesta esperada (HTTP 200 OK):**
```json
[
  {
    "id": 2,
    "username": "admin",
    "success": false,
    "timestamp": "2026-11-26 22:15:31"
  },
  {
    "id": 1,
    "username": "admin",
    "success": true,
    "timestamp": "2026-11-26 22:15:24"
  }
]
```

---

### 3. Orders API (Go - Puerto 8002)

#### 3.1 Crear un pedido (`POST /orders`)
```bash
curl -X POST http://localhost:8002/orders \
  -H "Content-Type: application/json" \
  -d '{"customer_name": "Ana Perez", "item_description": "Laptop Gamer 16GB", "amount": 1250.00, "status": "PENDING"}'
```
**Respuesta esperada (HTTP 201 Created):**
```json
{
  "id": 1,
  "customer_name": "Ana Perez",
  "item_description": "Laptop Gamer 16GB",
  "amount": 1250.0,
  "status": "PENDING",
  "created_at": "2026-11-26T22:16:00Z"
}
```

#### 3.2 Consultar todos los pedidos (`GET /orders`)
```bash
curl -X GET http://localhost:8002/orders
```
**Respuesta esperada (HTTP 200 OK):**
```json
[
  {
    "id": 1,
    "customer_name": "Ana Perez",
    "item_description": "Laptop Gamer 16GB",
    "amount": 1250.0,
    "status": "PENDING",
    "created_at": "2026-11-26T22:16:00Z"
  }
]
```

#### 3.3 Consultar un pedido por ID (`GET /orders/1`)
```bash
curl -X GET http://localhost:8002/orders/1
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "id": 1,
  "customer_name": "Ana Perez",
  "item_description": "Laptop Gamer 16GB",
  "amount": 1250.0,
  "status": "PENDING",
  "created_at": "2026-11-26T22:16:00Z"
}
```

#### 3.4 Actualizar el estado de un pedido (`PUT /orders/1`)
```bash
curl -X PUT http://localhost:8002/orders/1 \
  -H "Content-Type: application/json" \
  -d '{"status": "COMPLETED"}'
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "id": 1,
  "customer_name": "Ana Perez",
  "item_description": "Laptop Gamer 16GB",
  "amount": 1250.0,
  "status": "COMPLETED",
  "created_at": "2026-11-26T22:16:00Z"
}
```

#### 3.5 Eliminar un pedido (`DELETE /orders/1`)
```bash
curl -X DELETE http://localhost:8002/orders/1
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "message": "Order 1 deleted successfully"
}
```

---

### 4. Products API (JavaScript - Puerto 8003)

#### 4.1 Registrar un producto (`POST /products`)
```bash
curl -X POST http://localhost:8003/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Monitor Curvo 27", "price": 289.99, "description": "IPS 144Hz 1ms", "stock": 10}'
```
**Respuesta esperada (HTTP 201 Created):**
```json
{
  "id": 1,
  "name": "Monitor Curvo 27",
  "price": 289.99,
  "description": "IPS 144Hz 1ms",
  "stock": 10
}
```

#### 4.2 Consultar la lista de productos (`GET /products`)
```bash
curl -X GET http://localhost:8003/products
```
**Respuesta esperada (HTTP 200 OK):**
```json
[
  {
    "id": 1,
    "name": "Monitor Curvo 27",
    "price": 289.99,
    "description": "IPS 144Hz 1ms",
    "stock": 10
  }
]
```

#### 4.3 Consultar un producto por ID (`GET /products/1`)
```bash
curl -X GET http://localhost:8003/products/1
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "id": 1,
  "name": "Monitor Curvo 27",
  "price": 289.99,
  "description": "IPS 144Hz 1ms",
  "stock": 10
}
```

#### 4.4 Actualizar información de un producto (`PUT /products/1`)
```bash
curl -X PUT http://localhost:8003/products/1 \
  -H "Content-Type: application/json" \
  -d '{"price": 269.99, "stock": 18}'
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "id": 1,
  "name": "Monitor Curvo 27",
  "price": 269.99,
  "description": "IPS 144Hz 1ms",
  "stock": 18
}
```

#### 4.5 Eliminar un producto (`DELETE /products/1`)
```bash
curl -X DELETE http://localhost:8003/products/1
```
**Respuesta esperada (HTTP 200 OK):**
```json
{
  "message": "Product 1 deleted successfully"
}
```

---

## 💾 Demostración de Persistencia entre Reinicios

Para evidenciar que la información almacenada en las bases de datos SQLite **no se pierde** al detener o eliminar los contenedores:

1. **Registra datos** ejecutando el script `.\test-endpoints.ps1` o mediante `curl`.
2. **Detén y elimina los contenedores**:
   ```bash
   docker compose down
   ```
   *(Este comando apaga y remueve los 4 contenedores y la red, pero conserva los 4 volúmenes persistentes).*
3. **Vuelve a levantar los contenedores**:
   ```bash
   docker compose up -d
   ```
4. **Vuelve a consultar los datos**:
   ```bash
   curl -X GET http://localhost:8000/users
   curl -X GET http://localhost:8002/orders
   curl -X GET http://localhost:8003/products
   ```
5. **Resultado**: Todos los registros creados con anterioridad continúan existiendo exactamente igual, ya que residen en los volúmenes `users-data`, `auth-data`, `orders-data` y `products-data`.

> **Nota para reiniciar desde cero:** Si en algún momento deseas borrar completamente los datos de prueba y los volúmenes, puedes ejecutar:
> ```bash
> docker compose down -v
> ```

---

## ☁️ Guía de Despliegue en AWS (EC2)

La arquitectura es sumamente ligera (utiliza imágenes compiladas y binarios optimizados), lo cual permite ejecutarla perfectamente en una instancia básica de capa gratuita de AWS:

### 1. Lanzar una Instancia EC2
- **AMI**: Ubuntu Server 22.04 LTS o 24.04 LTS.
- **Tipo de Instancia**: `t2.micro` o `t3.micro` (Capa gratuita).
- **Par de llaves (Key Pair)**: Selecciona o crea tu llave `.pem` para conectarte por SSH.

### 2. Configurar el Grupo de Seguridad (Security Group)
En las **Reglas de Entrada (Inbound Rules)** de tu instancia, asegúrate de permitir:
- **SSH**: Puerto `22` (Origen: `Mi IP` o `0.0.0.0/0`).
- **Custom TCP**: Puerto `8000` (Users API).
- **Custom TCP**: Puerto `8001` (Auth API).
- **Custom TCP**: Puerto `8002` (Orders API).
- **Custom TCP**: Puerto `8003` (Products API).

### 3. Conectarte a la Instancia por SSH
```bash
ssh -i tu-llave.pem ubuntu@<IP_PUBLICA_EC2>
```

### 4. Instalar Docker y Git en la Instancia
Ejecuta en la terminal de tu instancia:
```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 git
sudo usermod -aG docker $USER
newgrp docker
```

### 5. Clonar el Repositorio y Levantar los Servicios
```bash
git clone <URL_DE_TU_REPOSITORIO> microservicios
cd microservicios
docker compose up --build -d
```

### 6. Validar desde tu Navegador o Postman
Una vez levantado en AWS, puedes acceder directamente usando la **IP Pública** de tu servidor EC2:
- `http://<IP_PUBLICA_EC2>:8000/users`
- `http://<IP_PUBLICA_EC2>:8001/health`
- `http://<IP_PUBLICA_EC2>:8002/orders`
- `http://<IP_PUBLICA_EC2>:8003/products`
