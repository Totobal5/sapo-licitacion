# Arquitectura del Proyecto

## 📁 Estructura de Directorios

```
licitaciones-sapo/
├── src/
│   ├── main/
│   │   ├── java/cl/sapo/licitaciones/
│   │   │   ├── MercadoPublicoMonitorApplication.java  # Main class
│   │   │   ├── config/
│   │   │   │   └── RestClientConfig.java              # Configuración RestClient
│   │   │   ├── controller/
│   │   │   │   ├── RssController.java                 # Endpoint RSS /rss
│   │   │   │   └── WebController.java                 # Interfaz web /
│   │   │   ├── dto/
│   │   │   │   ├── CompradorDTO.java                  # Record: Comprador
│   │   │   │   ├── ItemDTO.java                       # Record: Item individual
│   │   │   │   ├── ItemsContainerDTO.java             # Record: Wrapper de items
│   │   │   │   ├── LicitacionDTO.java                 # Record: Licitación
│   │   │   │   └── LicitacionApiResponse.java         # Record: Respuesta API
│   │   │   ├── entity/
│   │   │   │   ├── ItemLicitacion.java                # Entidad: Item persistido
│   │   │   │   └── Licitacion.java                    # Entidad: Licitación persistida
│   │   │   ├── repository/
│   │   │   │   ├── LicitacionRepository.java          # JPA Repository
│   │   │   │   └── LicitacionSpecs.java               # Specifications (ILIKE)
│   │   │   └── service/
│   │   │       ├── LicitacionService.java             # Lógica de búsqueda
│   │   │       └── SyncService.java                   # Sincronización scheduled
│   │   └── resources/
│   │       ├── application.properties                 # Configuración principal
│   │       └── templates/
│   │           └── index.html                         # Template Thymeleaf
│   └── test/
│       └── java/
├── database/
│   ├── init.sql                                       # Script creación BD
│   └── queries.sql                                    # Consultas útiles
├── pom.xml                                            # Maven dependencies
├── README.md                                          # Documentación principal
├── USAGE.md                                           # Guía de uso
└── .gitignore

```

## 🏗️ Arquitectura de Capas

### 1. **Controller Layer** (Presentación)

- **RssController**: Genera feed RSS 2.0 en XML
  - `GET /rss?q=texto&region=Region`
  - Produce: `application/xml`
  
- **WebController**: Interfaz web con Thymeleaf
  - `GET /`: Página principal con tabla
  - `POST /sync`: Sincronización manual

### 2. **Service Layer** (Lógica de Negocio)

- **SyncService**: 
  - Consume API de MercadoPublico cada hora
  - Filtra licitaciones (status=5, fecha válida)
  - Mapea DTOs → Entidades
  - Persiste en PostgreSQL
  
- **LicitacionService**:
  - Búsqueda con filtros (JPA Specifications)
  - Queries optimizadas con ILIKE

### 3. **Repository Layer** (Persistencia)

- **LicitacionRepository**: 
  - Extiende `JpaRepository` y `JpaSpecificationExecutor`
  - Métodos custom: `findByRegionIgnoreCase`
  
- **LicitacionSpecs**:
  - `searchByText()`: Búsqueda en múltiples campos con JOIN
  - `hasRegion()`: Filtro case-insensitive
  - `searchWithFilters()`: Combinación de specs

### 4. **Entity Layer** (Modelo de Datos)

```
Licitacion (1) ----< (N) ItemLicitacion
```

- **Licitacion**:
  - `@Id`: codigoExterno (String)
  - Campos: nombre, descripción, región, fechaCierre
  - `@OneToMany`: items
  - Flattening: región extraída de `Comprador.RegionUnidad`

- **ItemLicitacion**:
  - `@Id`: auto-generado
  - `@ManyToOne`: licitacion

### 5. **DTO Layer** (Transferencia de Datos)

Todos son **Java Records** (inmutables):

```
LicitacionApiResponse
└── List<LicitacionDTO>
    ├── CompradorDTO (regionUnidad)
    └── ItemsContainerDTO
        └── List<ItemDTO>
```

## 🔄 Flujo de Datos

### Sincronización (Hourly)

```
1. @Scheduled → SyncService.syncTenders()
2. RestClient → GET api.mercadopublico.cl/licitaciones.json
3. JSON → LicitacionApiResponse (DTO)
4. Stream Filter → codigoEstado=5 AND fechaCierre > now()
5. Mapper → DTO → Entity (flatten)
6. Repository → saveAll()
7. PostgreSQL ← Persist
```

### Búsqueda Web

```
1. User → GET /?q=texto&region=Region
2. WebController → LicitacionService.searchTenders()
3. LicitacionSpecs → JPA Criteria Query (ILIKE)
4. PostgreSQL → SELECT ... WHERE LOWER(name) LIKE '%texto%'
5. List<Licitacion> → Thymeleaf template
6. HTML → Browser
```

### RSS Feed

```
1. Miniflux → GET /rss?q=texto&region=Region
2. RssController → LicitacionService.searchTenders()
3. List<Licitacion> → generateRssFeed()
4. String XML (RSS 2.0) → Response
5. Miniflux ← Parse XML
```

## 🔍 Búsqueda con ILIKE

**PostgreSQL native, NO Regex en Java:**

```java
// LicitacionSpecs.java
Predicate nombrePredicate = builder.like(
    builder.lower(root.get("nombre")),
    "%" + query.toLowerCase() + "%"
);
```

**SQL generado:**

```sql
SELECT * FROM tenders t
LEFT JOIN tender_items ti ON t.external_code = ti.tender_code
WHERE 
  LOWER(t.name) LIKE '%software%'
  OR LOWER(t.description) LIKE '%software%'
  OR LOWER(ti.description) LIKE '%software%'
  OR LOWER(ti.product_name) LIKE '%software%'
```

## 📊 Modelo de Base de Datos

### Tabla: `tenders`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| external_code | VARCHAR(255) PK | Código único de licitación |
| name | VARCHAR(500) | Nombre de la licitación |
| description | TEXT | Descripción detallada |
| status_code | INTEGER | Estado (5 = Publicada) |
| close_date | TIMESTAMP | Fecha de cierre |
| publication_date | TIMESTAMP | Fecha de publicación |
| region | VARCHAR(255) | Región (de Comprador.RegionUnidad) |
| buyer_name | VARCHAR(500) | Nombre del comprador |
| buyer_rut | VARCHAR(50) | RUT del comprador |
| created_at | TIMESTAMP | Fecha inserción en BD |
| updated_at | TIMESTAMP | Última actualización |

**Índices:**
- `idx_tender_code`: external_code
- `idx_tender_status`: status_code
- `idx_tender_region`: region
- `idx_tender_close_date`: close_date

### Tabla: `tender_items`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | BIGSERIAL PK | ID autoincremental |
| product_code | VARCHAR(255) | Código de producto |
| product_name | VARCHAR(500) | Nombre del producto |
| description | TEXT | Descripción del ítem |
| quantity | INTEGER | Cantidad solicitada |
| unit_of_measure | VARCHAR(100) | Unidad de medida |
| tender_code | VARCHAR(255) FK | Referencia a tender |

## 🎯 Decisiones de Diseño

### ✅ Por qué Java Records para DTOs

- Inmutabilidad garantizada
- Menos boilerplate (no getters/setters/equals/hashCode)
- Claridad de intención (solo transferencia de datos)

### ✅ Por qué JPA Specifications

- Type-safe (vs. String queries)
- Composable (combinar filtros dinámicamente)
- ILIKE nativo de PostgreSQL (case-insensitive eficiente)

### ✅ Por qué RestClient (no RestTemplate)

- API moderna de Spring 6+
- Fluent API más legible
- Soporte nativo de Java 21

### ✅ Por qué Flattening en Entidades

- Reducir JOINs innecesarios en consultas
- Simplificar búsquedas (región en tabla principal)
- Mejor performance en queries frecuentes

### ✅ Por qué Thymeleaf (no React/Vue)

- Monolito simple (menos complejidad)
- SSR (Server-Side Rendering)
- No requiere build frontend

## 🔒 Consideraciones de Seguridad

1. **SQL Injection**: Protegido por JPA Criteria API
2. **XSS**: Thymeleaf escapa HTML automáticamente
3. **API Key**: En properties (no hardcoded)
4. **RSS XML**: Escapado con `escapeXml()` method

## 🚀 Optimizaciones

1. **Índices PostgreSQL**: En campos frecuentemente filtrados
2. **@Transactional(readOnly = true)**: En queries de lectura
3. **Lazy Loading**: `@OneToMany(fetch = FetchType.LAZY)`
4. **Distinct en JOIN**: Evitar duplicados en búsqueda de texto
5. **Batch Size**: Para `saveAll()` en sincronización

---

**Última actualización:** Diciembre 2025
