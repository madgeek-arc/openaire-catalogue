package eu.openaire.catalogue.resource;

import gr.uoa.di.madgik.catalogue.domain.Model;
import gr.uoa.di.madgik.catalogue.domain.SnippetTemplate;
import gr.uoa.di.madgik.catalogue.service.ModelResourceTypeMapper;
import gr.uoa.di.madgik.catalogue.service.SnippetTemplateCache;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.service.FacetLabelService;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Runs against the OpenAIRE service model and the index fields mapped from it.
 */
class RecordGroupsTest {

    private final GenericResourceService resources = mock(GenericResourceService.class);
    private final ResourceTypeService resourceTypes = mock(ResourceTypeService.class);
    private final FacetLabelService labels = mock(FacetLabelService.class);
    private final SnippetTemplateCache snippetTemplateCache = mock(SnippetTemplateCache.class);
    private final RecordGroups groups = new RecordGroups(resources, resourceTypes, labels, snippetTemplateCache);

    @BeforeEach
    void setUp() throws IOException {
        Model model;
        try (InputStream in = getClass().getResourceAsStream("/openaire/models/m-oa-service.json")) {
            model = new ObjectMapper().readValue(in, Model.class);
        }
        ResourceType resourceType = new ModelResourceTypeMapper().map(model, new ResourceType());
        when(resourceTypes.getResourceTypeIndexFields("service"))
                .thenReturn(new LinkedHashSet<>(resourceType.getIndexFields()));
        when(snippetTemplateCache.find("service")).thenReturn(Optional.of(SnippetTemplate.of(model)));
    }

    @Test
    void recordsAreGroupedByTheLabelsOfTheFacetValues() {
        Map<String, List<Map<String, Object>>> byValue = new LinkedHashMap<>();
        byValue.put("portfolios-research", List.of(service("zeta", "Zeta"), service("alpha", "alpha")));
        byValue.put("portfolios-research-old", List.of(service("alpha", "alpha"), service("beta", "Beta")));
        byValue.put("portfolios-publishing", List.of(service("zeta", "Zeta")));
        byValue.put("portfolios-unknown", List.of(service("beta", "Beta")));
        when(resources.getResultsGrouped(any(), eq("portfolios"))).thenReturn((Map) byValue);
        doAnswer(call -> {
            List<Facet> facets = call.getArgument(0);
            Map<String, String> names = Map.of("portfolios-research", "Research",
                    "portfolios-research-old", "Research", "portfolios-publishing", "Publishing");
            facets.getFirst().getValues().forEach(value -> value.setLabel(names.get(value.getValue())));
            return null;
        }).when(labels).enrichFacetLabels(any(), eq("service"));

        FacetFilter filter = new FacetFilter();
        Map<String, List<Map<String, Object>>> grouped = groups.group(ResourceKind.SERVICE, filter, "portfolios");

        assertEquals("service", filter.getResourceType());
        assertEquals(List.of("portfolios-unknown", "Publishing", "Research"), List.copyOf(grouped.keySet()));
        assertEquals(List.of("alpha", "beta", "zeta"), ids(grouped.get("Research")));
        assertEquals(List.of("zeta"), ids(grouped.get("Publishing")));
    }

    @Test
    void onlyFacetsGroupRecords() {
        for (String field : List.of("name", "description", "unknown")) {
            ResourceException e = assertThrows(ResourceException.class,
                    () -> groups.group(ResourceKind.SERVICE, new FacetFilter(), field));
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
        }
        verify(resources, never()).getResultsGrouped(any(), any());
    }

    @Test
    void snippetsKeepTheIdTheNameAndTheFieldsTheModelIncludesInSnippets() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", "openaire.argos");
        payload.put("name", "Argos");
        payload.put("tagline", "Plan data");
        payload.put("resourceOrganisation", "openaire");
        payload.put("targetUsers", List.of("researchers"));
        payload.put("extras", Map.of(
                "image", "https://example.org/argos.png",
                "portfolios", List.of("portfolios-research"),
                "users", List.of("users-researchers"),
                "git", "https://example.org/argos.git",
                "paymentCategories", List.of(
                        Map.of("paymentTitle", "Free", "paymentIncludes", List.of("support")),
                        Map.of("paymentIncludes", List.of("hosting")))));

        Map<String, Object> snippet = groups.snippets(ResourceKind.SERVICE).apply(payload);

        assertEquals(Map.of(
                "id", "openaire.argos",
                "name", "Argos",
                "tagline", "Plan data",
                "extras", Map.of(
                        "image", "https://example.org/argos.png",
                        "portfolios", List.of("portfolios-research"),
                        "users", List.of("users-researchers"),
                        "paymentCategories", List.of(Map.of("paymentTitle", "Free")))), snippet);
    }

    @Test
    void snippetsNeedAModelDefiningTheRecordType() {
        when(snippetTemplateCache.find("provider")).thenReturn(Optional.empty());

        ResourceException e = assertThrows(ResourceException.class, () -> groups.snippets(ResourceKind.PROVIDER));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatus());
    }

    private static List<String> ids(List<Map<String, Object>> bundles) {
        return bundles.stream().map(Bundles::id).toList();
    }

    private static Map<String, Object> service(String id, String name) {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("id", id);
        bundle.put("service", Map.of("id", id, "name", name));
        return bundle;
    }
}
