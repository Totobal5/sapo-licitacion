-- Consultas útiles para análisis y monitoreo

-- 1. Contar licitaciones por región
SELECT 
    region,
    COUNT(*) as total_tenders,
    COUNT(DISTINCT EXTRACT(YEAR FROM close_date)) as years
FROM tenders
WHERE status_code = 5
GROUP BY region
ORDER BY total_tenders DESC;

-- 2. Licitaciones próximas a cerrar (próximos 7 días)
SELECT 
    external_code,
    name,
    region,
    close_date,
    EXTRACT(EPOCH FROM (close_date - NOW())) / 3600 as hours_remaining
FROM tenders
WHERE status_code = 5
  AND close_date > NOW()
  AND close_date < NOW() + INTERVAL '7 days'
ORDER BY close_date ASC;

-- 3. Top 10 productos más solicitados
SELECT 
    ti.product_name,
    COUNT(*) as occurrences,
    SUM(ti.quantity) as total_quantity
FROM tender_items ti
JOIN tenders t ON ti.tender_code = t.external_code
WHERE t.status_code = 5
GROUP BY ti.product_name
ORDER BY occurrences DESC
LIMIT 10;

-- 4. Licitaciones por mes
SELECT 
    TO_CHAR(publication_date, 'YYYY-MM') as month,
    COUNT(*) as total_tenders
FROM tenders
WHERE status_code = 5
GROUP BY TO_CHAR(publication_date, 'YYYY-MM')
ORDER BY month DESC;

-- 5. Compradores más activos
SELECT 
    buyer_name,
    COUNT(*) as total_tenders,
    region
FROM tenders
WHERE status_code = 5
  AND buyer_name IS NOT NULL
GROUP BY buyer_name, region
ORDER BY total_tenders DESC
LIMIT 20;

-- 6. Búsqueda de texto (ejemplo con ILIKE)
SELECT 
    external_code,
    name,
    region,
    close_date
FROM tenders
WHERE status_code = 5
  AND (
    LOWER(name) LIKE LOWER('%computador%')
    OR LOWER(description) LIKE LOWER('%computador%')
  )
ORDER BY close_date DESC;

-- 7. Licitaciones con más ítems
SELECT 
    t.external_code,
    t.name,
    t.region,
    COUNT(ti.id) as item_count
FROM tenders t
LEFT JOIN tender_items ti ON t.external_code = ti.tender_code
WHERE t.status_code = 5
GROUP BY t.external_code, t.name, t.region
ORDER BY item_count DESC
LIMIT 20;

-- 8. Estadísticas generales
SELECT 
    COUNT(*) as total_tenders,
    COUNT(DISTINCT region) as total_regions,
    COUNT(DISTINCT buyer_name) as total_buyers,
    MIN(close_date) as earliest_close,
    MAX(close_date) as latest_close
FROM tenders
WHERE status_code = 5;

-- 9. Limpiar licitaciones cerradas (mantenimiento)
-- DELETE FROM tenders WHERE close_date < NOW() - INTERVAL '30 days';

-- 10. Verificar duplicados
SELECT 
    external_code,
    COUNT(*) as count
FROM tenders
GROUP BY external_code
HAVING COUNT(*) > 1;
