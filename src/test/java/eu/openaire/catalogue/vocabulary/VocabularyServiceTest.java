package eu.openaire.catalogue.vocabulary;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.ResourceKind;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VocabularyServiceTest {

    private static final List<Map<String, Object>> ENTRIES = List.of(
            Map.of("id", "WW", "name", "World", "type", "Region"),
            Map.of("id", "GR", "name", "Greece", "type", "Country"),
            Map.of("id", "en", "name", "English", "type", "Language"));

    private final GenericResourceService resources = mock(GenericResourceService.class);
    private final BundleService bundles = mock(BundleService.class);
    private final VocabularyService service = new VocabularyService(resources, bundles);

    @BeforeEach
    void setUp() {
        when(resources.get(eq("vocabulary"), anyString())).thenAnswer(call -> {
            throw new ResourceNotFoundException(call.<String>getArgument(1), "vocabulary");
        });
        for (Map<String, Object> entry : ENTRIES) {
            doReturn(entry).when(resources).get("vocabulary", (String) entry.get("id"));
        }
        when(resources.getResults(any(FacetFilter.class))).thenAnswer(call -> {
            Object type = call.<FacetFilter>getArgument(0).getFilter().get("type");
            List<Map<String, Object>> results = ENTRIES.stream()
                    .filter(entry -> type == null || Objects.equals(type, entry.get("type")))
                    .toList();
            return new Paging<>(results.size(), 0, results.size(), results, List.of());
        });
    }

    @Test
    void geographicalAvailabilityIsARegionOrACountry() {
        assertEquals("WW", service.byTypeAndId("Geographical availability", "WW").get("id"));
        assertEquals("GR", service.byTypeAndId("Geographical availability", "GR").get("id"));
        assertThrows(ResourceNotFoundException.class, () -> service.byTypeAndId("Geographical availability", "en"));
        assertEquals(List.of("WW", "GR"),
                service.byType("Geographical availability", null).stream().map(e -> e.get("id")).toList());
    }

    @Test
    void anEntryOfAnotherTypeIsNotFound() {
        assertEquals("GR", service.byTypeAndId("Country", "GR").get("id"));
        assertThrows(ResourceNotFoundException.class, () -> service.byTypeAndId("Country", "WW"));
    }

    @Test
    void referenceTypesResolveRecordsAndHideInactiveServices() {
        when(bundles.get(ResourceKind.PROVIDER, "openaire")).thenReturn(Map.of("id", "openaire",
                "provider", Map.of("name", "OpenAIRE")));
        when(bundles.get(eq(ResourceKind.SERVICE), eq("openaire.openplato"))).thenReturn(Map.of(
                "id", "openaire.openplato", "active", false, "service", Map.of("name", "OpenPlato")));

        assertEquals("OpenAIRE", service.byTypeAndId("Provider", "openaire").get("name"));
        assertEquals("OpenAIRE", service.byTypeAndId("resourceProviders", "openaire").get("name"));
        assertThrows(ResourceNotFoundException.class, () -> service.byTypeAndId("Service", "openaire.openplato"));
    }

    @Test
    void anEntryNeedsAnIdANameAndAStoredType() {
        assertEquals(400, assertThrows(ResourceException.class,
                () -> service.add(Map.of("id", "FR", "type", "Country"))).getStatus().value());
        assertThrows(ResourceException.class, () -> service.add(Map.of("id", "x", "name", "X", "type", "Provider")));
        assertThrows(ResourceException.class,
                () -> service.add(Map.of("id", "x", "name", "X", "type", "Geographical availability")));
        verify(resources, never()).add(anyString(), anyMap(), anyBoolean());

        service.add(Map.of("id", "FR", "name", "France", "type", "Country"));
        verify(resources).add("vocabulary", Map.of("id", "FR", "name", "France", "type", "Country"), true);
    }

    @Test
    void theParentOfAnEntryMustExist() {
        assertThrows(ResourceException.class,
                () -> service.validate(Map.of("id", "x", "name", "X", "type", "Country", "parentId", "nowhere")));
        assertEquals("x", service.validate(Map.of("id", "x", "name", "X", "type", "Country", "parentId", "WW"))
                .get("id"));
    }

    @Test
    void anUpdateKeepsTheIdOfThePath() {
        assertThrows(ResourceException.class,
                () -> service.update("GR", Map.of("id", "FR", "name", "France", "type", "Country")));
        assertThrows(ResourceNotFoundException.class,
                () -> service.update("FR", Map.of("name", "France", "type", "Country")));

        service.update("GR", Map.of("name", "Hellas", "type", "Country"));
        verify(resources).update("vocabulary", Map.of("id", "GR", "name", "Hellas", "type", "Country"), true);
    }
}
