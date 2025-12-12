#!/bin/bash
# Script de inicialización automática para PostgreSQL
# Se ejecuta solo la primera vez que se crea el contenedor

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Habilitar extensión unaccent para búsqueda insensible a acentos
    CREATE EXTENSION IF NOT EXISTS unaccent;
    
    -- Verificar que se instaló correctamente
    SELECT extname, extversion FROM pg_extension WHERE extname = 'unaccent';
EOSQL

echo "✅ Extensión unaccent habilitada correctamente"
