package cl.sapo.licitaciones.controller;

import cl.sapo.licitaciones.dto.SearchRequestDTO;
import cl.sapo.licitaciones.entity.Licitacion;
import cl.sapo.licitaciones.service.LicitacionService;
import cl.sapo.licitaciones.service.SyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * Web controller for Thymeleaf views.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {

    private final LicitacionService licitacionService;
    private final SyncService syncService;

    /**
     * Home page with tender list and search form.
     * Now uses validated SearchRequestDTO to prevent malicious input.
     */
    @GetMapping("/")
    public String index(
            @Valid @ModelAttribute SearchRequestDTO searchRequest,
            BindingResult bindingResult,
            Model model) {

        // Handle validation errors
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors in search request: {}", bindingResult.getAllErrors());
            model.addAttribute("tenders", List.of());
            model.addAttribute("searchQuery", "");
            model.addAttribute("searchRegion", "");
            model.addAttribute("sortBy", "close_date");
            model.addAttribute("rssUrl", "/rss");
            model.addAttribute("totalCount", 0);
            model.addAttribute("validationError", "Parámetros de búsqueda inválidos");
            return "index";
        }

        String q = searchRequest.getQueryOrDefault();
        String region = searchRequest.getRegionOrDefault();
        String sortBy = searchRequest.getSortByOrDefault();

        log.info("Accessing index page with q='{}', region='{}', sortBy='{}'", q, region, sortBy);

        List<Licitacion> tenders;

        if (q != null || region != null) {
            tenders = licitacionService.searchTenders(q, region, sortBy);
        } else {
            tenders = licitacionService.getAllTenders(sortBy);
        }

        // Build RSS URL
        String rssUrl = buildRssUrl(q, region);

        model.addAttribute("tenders", tenders);
        model.addAttribute("searchQuery", q != null ? q : "");
        model.addAttribute("searchRegion", region != null ? region : "");
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("rssUrl", rssUrl);
        model.addAttribute("totalCount", tenders.size());

        return "index";
    }

    /**
     * Manual sync trigger endpoint.
     */
    @PostMapping("/sync")
    public String triggerSync(RedirectAttributes redirectAttributes) {
        log.info("Manual async sync triggered via web interface");

        try {
            syncService.triggerSyncAsync();
            redirectAttributes.addFlashAttribute("message", 
                "Sincronización iniciada en segundo plano. Refresca la página en unos minutos.");
            redirectAttributes.addFlashAttribute("messageType", "info");
        } catch (Exception e) {
            log.error("Error during manual sync", e);
            redirectAttributes.addFlashAttribute("message", "Error durante la sincronización: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/";
    }

    /**
     * Builds RSS feed URL with current filters.
     */
    private String buildRssUrl(String query, String region) {
        var builder = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/rss");

        if (query != null && !query.isBlank()) {
            builder.queryParam("q", query);
        }

        if (region != null && !region.isBlank()) {
            builder.queryParam("region", region);
        }

        return builder.toUriString();
    }
}
