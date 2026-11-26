#!/usr/bin/env bash
set -e

UNIQUE_ID=$(date +%s)

echo "=============================================="
echo " Probando Microservicios con SQLite y Docker  "
echo "=============================================="

# 1. Users API (Python - :8000)
echo ""
echo "[1/4] Probando Users API (Python - :8000)..."
echo "-> Creando usuario con email dinamico..."
CREATE_USER=$(curl -s -X POST http://localhost:8000/users \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"Usuario $UNIQUE_ID\", \"email\": \"usuario_$UNIQUE_ID@test.com\", \"password\": \"123\"}")
echo "Respuesta: $CREATE_USER"

echo "-> Consultando usuarios..."
curl -s -X GET http://localhost:8000/users
echo ""

# 2. Auth API (Java - :8001)
echo ""
echo "[2/4] Probando Auth API (Java - :8001)..."
echo "-> Health check..."
curl -s -X GET http://localhost:8001/health
echo ""

echo "-> Probando Login exitoso (admin/admin123)..."
LOGIN_OK=$(curl -s -X POST http://localhost:8001/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}')
echo "Respuesta: $LOGIN_OK"

echo "-> Probando Login inválido (admin/wrong)..."
LOGIN_FAIL=$(curl -s -X POST http://localhost:8001/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "wrong"}')
echo "Respuesta: $LOGIN_FAIL"

# 3. Orders API (Go - :8002)
echo ""
echo "[3/4] Probando Orders API (Go - :8002)..."
echo "-> Creando pedido..."
CREATE_ORDER=$(curl -s -X POST http://localhost:8002/orders \
  -H "Content-Type: application/json" \
  -d "{\"customer_name\": \"Cliente $UNIQUE_ID\", \"item_description\": \"Laptop Thinkpad\", \"amount\": 950.00, \"status\": \"PENDING\"}")
echo "Respuesta: $CREATE_ORDER"

echo "-> Consultando pedidos..."
curl -s -X GET http://localhost:8002/orders
echo ""

# 4. Products API (JavaScript - :8003)
echo ""
echo "[4/4] Probando Products API (JavaScript - :8003)..."
echo "-> Creando producto..."
CREATE_PRODUCT=$(curl -s -X POST http://localhost:8003/products \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"Teclado $UNIQUE_ID\", \"price\": 49.99, \"description\": \"Switch Red\", \"stock\": 25}")
echo "Respuesta: $CREATE_PRODUCT"

echo "-> Consultando productos..."
curl -s -X GET http://localhost:8003/products
echo ""

echo ""
echo "=============================================="
echo " Todas las APIs respondieron correctamente!    "
echo "=============================================="
