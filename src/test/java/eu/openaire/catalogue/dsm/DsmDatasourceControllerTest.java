package eu.openaire.catalogue.dsm;

import gr.uoa.di.madgik.registry.domain.Paging;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DsmDatasourceControllerTest {

    @Test
    void theHeaderGivesThePage() {
        Paging<Object> paging = DsmDatasourceController.paging(Map.of(
                "header", Map.of("page", "2", "size", "10", "total", "25"),
                "datasourceInfo", List.of("a", "b", "c", "d", "e")));

        assertEquals(20, paging.getFrom());
        assertEquals(24, paging.getTo());
        assertEquals(25, paging.getTotal());
        assertEquals(5, paging.getResults().size());
    }

    @Test
    void missingOrInvalidHeaderFieldsFallBackToTheResults() {
        Map<String, Object> header = new HashMap<>();
        header.put("page", null);
        header.put("size", "many");
        Paging<Object> paging = DsmDatasourceController.paging(Map.of(
                "header", header, "datasourceInfo", List.of("a", "b")));

        assertEquals(0, paging.getFrom());
        assertEquals(1, paging.getTo());
        assertEquals(2, paging.getTotal());
    }

    @Test
    void anEmptyPageEndsBeforeItStarts() {
        Paging<Object> paging = DsmDatasourceController.paging(Map.of(
                "header", Map.of("page", 0, "size", 10, "total", 0), "datasourceInfo", List.of()));

        assertEquals(0, paging.getFrom());
        assertEquals(-1, paging.getTo());
        assertEquals(0, paging.getTotal());
    }
}
