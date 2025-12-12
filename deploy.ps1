# ====================================
# Script de Deployment - Licitaciones SAPO
# ====================================
# PowerShell script para actualizar la aplicación en Docker

Write-Host "🚀 Iniciando deployment de Licitaciones SAPO..." -ForegroundColor Cyan

# Verificar que existe .env
if (-not (Test-Path ".env")) {
    Write-Host "❌ Error: Archivo .env no encontrado" -ForegroundColor Red
    Write-Host "   Copia .env.example a .env y configura tus variables" -ForegroundColor Yellow
    exit 1
}

# Verificar API ticket
$envContent = Get-Content .env -Raw
if ($envContent -match "YOUR_API_KEY_HERE") {
    Write-Host "⚠️  Advertencia: MERCADOPUBLICO_API_TICKET no está configurado" -ForegroundColor Yellow
    $continue = Read-Host "¿Deseas continuar? (s/N)"
    if ($continue -ne "s") {
        exit 0
    }
}

Write-Host ""
Write-Host "📦 Paso 1/4: Deteniendo contenedores actuales..." -ForegroundColor Yellow
docker-compose down

Write-Host ""
Write-Host "🔨 Paso 2/4: Construyendo nueva imagen..." -ForegroundColor Yellow
docker-compose build --no-cache

Write-Host ""
Write-Host "🚀 Paso 3/4: Iniciando servicios..." -ForegroundColor Yellow
docker-compose up -d

Write-Host ""
Write-Host "⏳ Paso 4/4: Esperando que los servicios estén listos..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "📊 Estado de los contenedores:" -ForegroundColor Cyan
docker-compose ps

Write-Host ""
Write-Host "📝 Logs recientes:" -ForegroundColor Cyan
docker-compose logs --tail=20 app

Write-Host ""
Write-Host "✅ Deployment completado!" -ForegroundColor Green
Write-Host ""
Write-Host "🌐 Aplicación disponible en: http://localhost:8080" -ForegroundColor Cyan
Write-Host "📡 RSS Feed: http://localhost:8080/rss" -ForegroundColor Cyan
Write-Host ""
Write-Host "Para ver logs en tiempo real: docker-compose logs -f app" -ForegroundColor Gray
Write-Host "Para detener: docker-compose down" -ForegroundColor Gray
