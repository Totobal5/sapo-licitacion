# **🐸 Sapo Licitacion (Mercado Publico Monitor)**

**"Sapo"**: *Chilenismo para persona curiosa o vigilante.*

Sistema de monitoreo automatizado para licitaciones de **Mercado Público Chile**. Descarga, filtra, almacena y expone licitaciones vía Web y RSS para integración con lectores de noticias (como Miniflux).

## **📋 Tabla de Contenidos**

1. [Características Principales](https://www.google.com/search?q=%23-caracter%C3%ADsticas-principales)  
2. [Arquitectura y Stack](https://www.google.com/search?q=%23-arquitectura-y-stack)  
3. [Configuración Rápida (Docker)](https://www.google.com/search?q=%23-configuraci%C3%B3n-r%C3%A1pida-docker)  
4. [Despliegue en Orange Pi (Portainer)](https://www.google.com/search?q=%23-despliegue-en-orange-pi-portainer)  
5. [Despliegue en Railway](https://www.google.com/search?q=%23-despliegue-en-railway-cloud)  
6. [Desarrollo Local](https://www.google.com/search?q=%23-desarrollo-local)  
7. [Uso y RSS](https://www.google.com/search?q=%23-uso-y-rss)  
8. [Variables de Entorno](https://www.google.com/search?q=%23-variables-de-entorno)

## **🎯 Características Principales**

* 🔄 **Sincronización Automática**: Tarea programada (@Scheduled) que descarga licitaciones cada hora.  
* 🧹 **Limpieza Inteligente**: Elimina licitaciones cerradas, desiertas o revocadas para mantener la BD optimizada.  
* 🔍 **Búsqueda Avanzada**: Búsqueda insensible a mayúsculas y acentos (unaccent \+ ILIKE) en Postgres.  
* 📡 **Feed RSS 2.0**: Endpoint /rss compatible con Feedly, Miniflux, etc.  
* 📱 **Interfaz Web Simple**: Tabla con filtros y enlaces directos a las fichas de licitación.  
* 🐳 **Docker Ready**: Configuración Multi-stage build para imágenes ligeras (Alpine).

## **🏗 Arquitectura y Stack**

El proyecto es un **Monolito Modular** construido con:

* **Lenguaje**: Java 21 (LTS) usando Records para DTOs inmutables.  
* **Framework**: Spring Boot 3 (Web, Data JPA, Validation).  
* **Base de Datos**: PostgreSQL 16 (con extensión unaccent).  
* **Frontend**: Thymeleaf (Server Side Rendering) \+ Bootstrap.  
* **Cliente HTTP**: RestClient (Spring 6).

### **Flujo de Datos**

1. **Ingesta**: Cron Job \-\> API Mercado Público \-\> Filtrado (Solo "Publicadas") \-\> BD.  
2. **Consulta**: Usuario/RSS \-\> Repository \-\> JPA Specifications \-\> BD.

## **🚀 Configuración Rápida (Docker)**

La forma más fácil de iniciar es con Docker Compose.

### **1\. Requisitos**

* Docker y Docker Compose instalados.  
* Ticket (API Key) de Mercado Público.

### **2\. Archivo docker-compose.yml**

Crea un archivo con este contenido en la raíz:

services:  
  db:  
    image: postgres:16-alpine  
    container\_name: sapo-db  
    environment:  
      POSTGRES\_DB: sapolicitacion  
      POSTGRES\_USER: postgres  
      POSTGRES\_PASSWORD: tu\_password\_local  
    volumes:  
      \- postgres\_data:/var/lib/postgresql/data  
    ports:  
      \- "5432:5432"  
    restart: unless-stopped  
    healthcheck:  
      test: \["CMD-SHELL", "pg\_isready \-U postgres"\]  
      interval: 5s  
      timeout: 5s  
      retries: 5

  app:  
    build: .  
    container\_name: sapo-app  
    ports:  
      \- "8080:8080"  
    environment:  
      SPRING\_DATASOURCE\_URL: jdbc:postgresql://db:5432/sapolicitacion  
      SPRING\_DATASOURCE\_USERNAME: postgres  
      SPRING\_DATASOURCE\_PASSWORD: tu\_password\_local  
      MERCADOPUBLICO\_API\_TICKET: TU\_TICKET\_REAL\_AQUI  
    depends\_on:  
      condition: service\_healthy  
      db:  
    restart: unless-stopped

volumes:  
  postgres\_data:

### **3\. Ejecutar**

docker-compose up \-d \--build

Accede a: http://localhost:8080

## **🍊 Despliegue en Orange Pi (Portainer)**

Optimizado para arquitectura **ARM64**.

1. Accede a tu Portainer (ej: http://orange-pi:9000).  
2. Ve a **Stacks** \> **Add Stack**.  
3. Nombra el stack: sapo-licitacion.  
4. Pega el contenido del docker-compose.yml de arriba en el editor web.  
5. **Importante**: Cambia MERCADOPUBLICO\_API\_TICKET por tu clave real.  
6. Haz clic en **Deploy the stack**.

**Nota**: El Dockerfile del proyecto usa una imagen base multi-arquitectura (eclipse-temurin:21-jre-alpine), por lo que funcionará nativamente en Raspberry Pi/Orange Pi sin cambios.

## **☁️ Despliegue en Railway (Cloud)**

1. Crea un nuevo proyecto en [Railway](https://railway.app/).  
2. Selecciona "Deploy from GitHub repo" y elige este repositorio.  
3. Agrega un servicio de **PostgreSQL** dentro del mismo proyecto.  
4. En el servicio de tu aplicación (Spring Boot), configura las **Variables de Entorno**:  
   * MERCADOPUBLICO\_API\_TICKET: Tu ticket.  
   * SPRING\_DATASOURCE\_URL: ${{Postgres.DATABASE\_URL}} (Railway autocompleta esto).  
   * SPRING\_DATASOURCE\_USERNAME: ${{Postgres.PGUSER}}  
   * SPRING\_DATASOURCE\_PASSWORD: ${{Postgres.PGPASSWORD}}  
   * PORT: 8080 (Opcional, Railway lo inyecta solo).  
5. Railway detectará el Dockerfile y desplegará automáticamente.

## **💻 Desarrollo Local**

Si prefieres correrlo sin Docker (requiere Java 21 y Maven):

\# 1\. Levanta solo la base de datos (o usa una local)  
docker run \--name pg-dev \-e POSTGRES\_PASSWORD=pass \-p 5432:5432 \-d postgres:16-alpine

\# 2\. Configura tu API Key  
export MERCADOPUBLICO\_API\_TICKET="TU-TICKET"

\# 3\. Compila y Ejecuta  
./mvnw spring-boot:run

## **📡 Uso y RSS**

### **Web UI**

Visita la raíz (/) para ver la tabla de licitaciones vigentes, buscar por texto y filtrar por región.

### **RSS Feed (Integración Miniflux)**

El sistema expone un feed estándar compatible con cualquier lector.

* **Feed General**: http://tu-servidor:8080/rss  
* **Filtrado por Texto**: http://tu-servidor:8080/rss?q=computadores  
* **Filtrado por Región**: http://tu-servidor:8080/rss?region=Valparaiso  
* **Combinado**: http://tu-servidor:8080/rss?region=Metropolitana\&q=servidor

Copia estas URLs y agrégalas a tu Miniflux.

## **🔧 Variables de Entorno**

| Variable | Descripción | Default |
| :---- | :---- | :---- |
| MERCADOPUBLICO\_API\_TICKET | **Requerido**. Tu API Key. | \- |
| SPRING\_DATASOURCE\_URL | URL JDBC de conexión. | jdbc:postgresql://localhost:5432/sapolicitacion |
| SPRING\_DATASOURCE\_USERNAME | Usuario de la BD. | postgres |
| SPRING\_DATASOURCE\_PASSWORD | Password de la BD. | postgres |
| SERVER\_PORT | Puerto de la aplicación web. | 8080 |

## **🛠 Comandos Útiles (Troubleshooting)**

**Ver logs en tiempo real (Docker):**

docker logs \-f sapo-app

**Entrar a la base de datos dentro del contenedor:**

docker exec \-it sapo-db psql \-U postgres \-d sapolicitacion

**Forzar reconstrucción de imagen:**

docker-compose build \--no-cache

*Generado para el proyecto Sapo Licitacion \- 2026*