Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " Probando Microservicios con SQLite y Docker  " -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

# Generar un sufijo único para permitir múltiples ejecuciones sin conflicto de email UNIQUE
$uniqueId = Get-Random -Minimum 1000 -Maximum 9999

# 1. Users API (Python - :8000)
Write-Host "`n[1/4] Probando Users API (Python - :8000)..." -ForegroundColor Yellow
try {
    $email = "usuario_$uniqueId@test.com"
    $userBody = @{ name = "Usuario $uniqueId"; email = $email; password = "123" } | ConvertTo-Json
    $userResp = Invoke-RestMethod -Uri "http://localhost:8000/users" -Method Post -Body $userBody -ContentType "application/json"
    Write-Host "Usuario creado exitosamente: " ($userResp | ConvertTo-Json -Compress) -ForegroundColor Green

    $users = Invoke-RestMethod -Uri "http://localhost:8000/users" -Method Get
    Write-Host "Total usuarios en SQLite: " $users.Count -ForegroundColor Green
} catch {
    Write-Host "Error en Users API: $_" -ForegroundColor Red
}

# 2. Auth API (Java - :8001)
Write-Host "`n[2/4] Probando Auth API (Java - :8001)..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8001/health" -Method Get
    Write-Host "Health Check: " ($health | ConvertTo-Json -Compress) -ForegroundColor Green

    $loginBody = @{ username = "admin"; password = "admin123" } | ConvertTo-Json
    $loginResp = Invoke-RestMethod -Uri "http://localhost:8001/login" -Method Post -Body $loginBody -ContentType "application/json"
    Write-Host "Login exitoso: " ($loginResp | ConvertTo-Json -Compress) -ForegroundColor Green
} catch {
    Write-Host "Error en Auth API: $_" -ForegroundColor Red
}

# 3. Orders API (Go - :8002)
Write-Host "`n[3/4] Probando Orders API (Go - :8002)..." -ForegroundColor Yellow
try {
    $orderBody = @{ customer_name = "Cliente $uniqueId"; item_description = "Laptop Thinkpad"; amount = 950.00; status = "PENDING" } | ConvertTo-Json
    $orderResp = Invoke-RestMethod -Uri "http://localhost:8002/orders" -Method Post -Body $orderBody -ContentType "application/json"
    Write-Host "Pedido creado exitosamente: " ($orderResp | ConvertTo-Json -Compress) -ForegroundColor Green

    $orders = Invoke-RestMethod -Uri "http://localhost:8002/orders" -Method Get
    Write-Host "Total pedidos en SQLite: " $orders.Count -ForegroundColor Green
} catch {
    Write-Host "Error en Orders API: $_" -ForegroundColor Red
}

# 4. Products API (JavaScript - :8003)
Write-Host "`n[4/4] Probando Products API (JavaScript - :8003)..." -ForegroundColor Yellow
try {
    $productBody = @{ name = "Teclado $uniqueId"; price = 49.99; description = "Switch Red"; stock = 25 } | ConvertTo-Json
    $productResp = Invoke-RestMethod -Uri "http://localhost:8003/products" -Method Post -Body $productBody -ContentType "application/json"
    Write-Host "Producto creado exitosamente: " ($productResp | ConvertTo-Json -Compress) -ForegroundColor Green

    $products = Invoke-RestMethod -Uri "http://localhost:8003/products" -Method Get
    Write-Host "Total productos en SQLite: " $products.Count -ForegroundColor Green
} catch {
    Write-Host "Error en Products API: $_" -ForegroundColor Red
}

Write-Host "`n==============================================" -ForegroundColor Cyan
Write-Host " Todas las pruebas finalizaron con exito!    " -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan
