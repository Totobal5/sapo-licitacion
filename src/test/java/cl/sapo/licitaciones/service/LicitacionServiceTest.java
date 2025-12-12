package cl.sapo.licitaciones.service;

import cl.sapo.licitaciones.entity.Licitacion;
import cl.sapo.licitaciones.repository.LicitacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for LicitacionService.
 */
@SpringBootTest
@ActiveProfiles("test")
class LicitacionServiceTest {

    @Autowired
    private LicitacionService licitacionService;

    @Autowired
    private LicitacionRepository licitacionRepository;

    @Test
    void contextLoads() {
        assertThat(licitacionService).isNotNull();
    }

    @Test
    void testSearchTendersWithQuery() {
        // Given: Sample data in database
        Licitacion tender = Licitacion.builder()
                .codigoExterno("TEST-001")
                .nombre("Compra de computadores")
                .descripcion("Adquisición de equipos de computación")
                .codigoEstado(5)
                .region("Metropolitana")
                .fechaCierre(LocalDateTime.now().plusDays(10))
                .build();

        licitacionRepository.save(tender);

        // When: Searching by text
        List<Licitacion> results = licitacionService.searchTenders("computadores", null);

        // Then: Should find the tender
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getNombre()).contains("computadores");

        // Cleanup
        licitacionRepository.deleteById("TEST-001");
    }

    @Test
    void testSearchTendersByRegion() {
        // Given: Sample data
        Licitacion tender = Licitacion.builder()
                .codigoExterno("TEST-002")
                .nombre("Test Tender")
                .codigoEstado(5)
                .region("Valparaíso")
                .fechaCierre(LocalDateTime.now().plusDays(5))
                .build();

        licitacionRepository.save(tender);

        // When: Searching by region
        List<Licitacion> results = licitacionService.searchTenders(null, "Valparaíso");

        // Then: Should find regional tenders
        assertThat(results).isNotEmpty();

        // Cleanup
        licitacionRepository.deleteById("TEST-002");
    }
}
