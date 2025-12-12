package cl.sapo.licitaciones.repository;

import cl.sapo.licitaciones.entity.Licitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Licitacion entity.
 * Extends JpaSpecificationExecutor for dynamic queries with Specifications.
 */
@Repository
public interface LicitacionRepository extends JpaRepository<Licitacion, String>, 
                                               JpaSpecificationExecutor<Licitacion> {

    /**
     * Find tenders by region (case insensitive).
     */
    List<Licitacion> findByRegionIgnoreCase(String region);

    /**
     * Check if a tender exists by external code.
     */
    boolean existsByCodigoExterno(String codigoExterno);
    
    /**
     * Delete a tender by external code.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Licitacion l WHERE l.codigoExterno = :codigoExterno")
    void deleteByCodigoExterno(String codigoExterno);
    
    /**
     * Delete tenders whose close date has passed.
     * Returns the number of deleted records.
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Licitacion l WHERE l.fechaCierre < :now")
    int deleteExpiredTenders(LocalDateTime now);
}
