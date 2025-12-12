package cl.sapo.licitaciones.repository;

import cl.sapo.licitaciones.entity.ItemLicitacion;
import cl.sapo.licitaciones.entity.Licitacion;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for Licitacion entity.
 * Uses PostgreSQL ILIKE and unaccent for case-insensitive and accent-insensitive text search.
 */
public class LicitacionSpecs {

    /**
     * Search by region (case and accent insensitive).
     */
    public static Specification<Licitacion> hasRegion(String region) {
        return (root, query, builder) -> {
            if (region == null || region.isBlank()) {
                return builder.conjunction();
            }
            // Using unaccent for accent-insensitive search
            Expression<String> unaccentRegion = builder.function(
                "unaccent", String.class, builder.lower(root.get("region"))
            );
            Expression<String> unaccentSearch = builder.function(
                "unaccent", String.class, builder.literal(region.toLowerCase())
            );
            return builder.like(unaccentRegion, builder.concat(builder.concat("%", unaccentSearch), "%"));
        };
    }

    /**
     * Search by text in multiple fields using unaccent (PostgreSQL native).
     * Searches in:
     * - Licitacion.nombre
     * - Licitacion.descripcion
     * - ItemLicitacion.descripcion (via JOIN)
     */
    public static Specification<Licitacion> searchByText(String query) {
        return (root, criteriaQuery, builder) -> {
            if (query == null || query.isBlank()) {
                return builder.conjunction();
            }

            String searchPattern = "%" + query.toLowerCase() + "%";

            // Join with items for searching in item descriptions
            Join<Licitacion, ItemLicitacion> itemsJoin = root.join("items", JoinType.LEFT);

            // Create predicates for each searchable field using unaccent
            Expression<String> unaccentNombre = builder.function(
                "unaccent", String.class, builder.lower(root.get("nombre"))
            );
            Expression<String> unaccentDescripcion = builder.function(
                "unaccent", String.class, builder.lower(root.get("descripcion"))
            );
            Expression<String> unaccentItemDesc = builder.function(
                "unaccent", String.class, builder.lower(itemsJoin.get("description"))
            );
            Expression<String> unaccentQuery = builder.function(
                "unaccent", String.class, builder.literal(searchPattern)
            );

            Predicate nombrePredicate = builder.like(unaccentNombre, unaccentQuery);
            Predicate descripcionPredicate = builder.like(unaccentDescripcion, unaccentQuery);
            Predicate itemDescripcionPredicate = builder.like(unaccentItemDesc, unaccentQuery);

            Expression<String> unaccentProductName = builder.function(
                "unaccent", String.class, builder.lower(itemsJoin.get("productName"))
            );
            Predicate itemProductNamePredicate = builder.like(unaccentProductName, unaccentQuery);

            // Combine with OR
            Predicate finalPredicate = builder.or(
                    nombrePredicate,
                    descripcionPredicate,
                    itemDescripcionPredicate,
                    itemProductNamePredicate
            );

            // Ensure distinct results (because of JOIN)
            criteriaQuery.distinct(true);

            return finalPredicate;
        };
    }

    /**
     * Filter by status code.
     */
    public static Specification<Licitacion> hasStatus(Integer statusCode) {
        return (root, query, builder) -> {
            if (statusCode == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("codigoEstado"), statusCode);
        };
    }

    /**
     * Combine multiple specifications with AND logic.
     */
    public static Specification<Licitacion> searchWithFilters(String query, String region) {
        return Specification.where(searchByText(query))
                .and(hasRegion(region))
                // Only published tenders
                .and(hasStatus(5));
    }
}
