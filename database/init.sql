-- Script SQL para crear la base de datos en PostgreSQL
-- Ejecutar como usuario postgres

-- Crear base de datos
CREATE DATABASE licitaciones_db
    WITH 
    ENCODING = 'UTF8'
    LC_COLLATE = 'Spanish_Chile.1252'
    LC_CTYPE = 'Spanish_Chile.1252'
    TEMPLATE = template0;

-- Conectar a la base de datos
\c licitaciones_db;

-- Habilitar extensión unaccent para búsqueda insensible a acentos
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Las tablas serán creadas automáticamente por Hibernate (ddl-auto=update)
-- Pero puedes crear manualmente si prefieres:

/*
CREATE TABLE tenders (
    external_code VARCHAR(255) PRIMARY KEY NOT NULL,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    status_code INTEGER NOT NULL,
    close_date TIMESTAMP,
    publication_date TIMESTAMP,
    region VARCHAR(255),
    buyer_name VARCHAR(500),
    buyer_rut VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE TABLE tender_items (
    id BIGSERIAL PRIMARY KEY,
    product_code VARCHAR(255),
    product_name VARCHAR(500) NOT NULL,
    description TEXT,
    quantity INTEGER,
    unit_of_measure VARCHAR(100),
    tender_code VARCHAR(255) NOT NULL,
    FOREIGN KEY (tender_code) REFERENCES tenders(external_code) ON DELETE CASCADE
);

-- Índices para mejorar performance
CREATE INDEX idx_tender_code ON tenders(external_code);
CREATE INDEX idx_tender_status ON tenders(status_code);
CREATE INDEX idx_tender_region ON tenders(region);
CREATE INDEX idx_tender_close_date ON tenders(close_date);
CREATE INDEX idx_tender_items_tender_code ON tender_items(tender_code);

-- Índices para búsqueda de texto (ILIKE es más eficiente con índices GIN)
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_tender_name_trgm ON tenders USING gin(name gin_trgm_ops);
CREATE INDEX idx_tender_description_trgm ON tenders USING gin(description gin_trgm_ops);
CREATE INDEX idx_item_description_trgm ON tender_items USING gin(description gin_trgm_ops);
CREATE INDEX idx_item_product_name_trgm ON tender_items USING gin(product_name gin_trgm_ops);
*/

-- Crear usuario de aplicación (opcional)
-- CREATE USER licitaciones_app WITH PASSWORD 'secure_password';
-- GRANT ALL PRIVILEGES ON DATABASE licitaciones_db TO licitaciones_app;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO licitaciones_app;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO licitaciones_app;
