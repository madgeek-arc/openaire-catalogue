package eu.openaire.catalogue.api;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.ResourceKind;
import eu.openaire.catalogue.security.AccessRules;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import gr.uoa.di.madgik.resourcecatalogue.domain.Datasource;
import gr.uoa.di.madgik.resourcecatalogue.domain.Provider;
import gr.uoa.di.madgik.resourcecatalogue.domain.Service;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The EOSC output is the payload read through the lot1 classes: lot1 fields survive, other fields are left out.
 */
class EoscControllerTest {

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final AccessRules access = mock(AccessRules.class);
    private final Authentication auth = mock(Authentication.class);
    private final EoscController controller = new EoscController(mock(BundleService.class),
            mock(ResourceTypeService.class), access, mapper, new OpenAIREProperties());

    @Test
    void aServiceKeepsItsLot1FieldsAndLosesTheRest() {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("id", "openaire.argos");
        service.put("name", "Argos");
        service.put("resourceOrganisation", "openaire");
        service.put("resourceProviders", List.of("openaire"));
        service.put("tags", List.of("dmp"));
        service.put("mainContact", Map.of("firstName", "A", "email", "contact@example.org"));
        service.put("scientificDomains", List.of(Map.of("scientificDomain", "d", "scientificSubdomain", "s")));
        service.put("extras", Map.of("users", List.of("users-researchers")));

        Service lot1 = controller.toService(Map.of("service", service));
        JsonNode json = mapper.valueToTree(lot1);

        assertEquals("openaire.argos", lot1.getId());
        assertEquals("openaire", lot1.getResourceOrganisation());
        assertEquals("contact@example.org", json.get("mainContact").get("email").asString());
        assertEquals("s", json.get("scientificDomains").get(0).get("scientificSubdomain").asString());
        assertTrue(json.has("tags"));
        assertFalse(json.has("extras"));
    }

    @Test
    void onlyThePayloadIsWrittenAndNotTheBundle() {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("id", "openaire.argos");
        bundle.put("status", "approved");
        bundle.put("active", true);
        bundle.put("suspended", false);
        bundle.put("metadata", Map.of("registeredBy", "someone"));
        bundle.put("loggingInfo", List.of(Map.of("type", "onboard", "actionType", "approved")));
        bundle.put("latestOnboardingInfo", Map.of("type", "onboard", "actionType", "approved"));
        bundle.put("properties", Map.of("channels", List.of("oa", "node")));
        bundle.put("service", Map.of("id", "openaire.argos", "name", "Argos", "resourceOrganisation", "openaire"));
        Map<String, Object> providerBundle = new LinkedHashMap<>(bundle);
        providerBundle.remove("service");
        providerBundle.put("provider", Map.of("id", "openaire", "name", "OpenAIRE"));

        for (JsonNode json : List.<JsonNode>of(mapper.valueToTree(controller.toService(bundle)),
                mapper.valueToTree(controller.toProvider(providerBundle, null)))) {
            for (String key : List.of("status", "active", "suspended", "metadata", "loggingInfo",
                    "latestOnboardingInfo", "properties", "service", "provider")) {
                assertFalse(json.has(key), key);
            }
        }
    }

    @Test
    void aServiceIsWrittenWithItsIdAtTheTopLevel() {
        BundleService bundles = mock(BundleService.class);
        EoscController eosc = new EoscController(bundles, mock(ResourceTypeService.class), access, mapper,
                new OpenAIREProperties());
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("id", "openaire.argos");
        bundle.put("active", true);
        bundle.put("properties", Map.of("channels", List.of("node")));
        bundle.put("service", Map.of("id", "openaire.argos", "name", "Argos"));
        when(bundles.get(ResourceKind.SERVICE, "openaire.argos")).thenReturn(bundle);
        when(bundles.search(eq(ResourceKind.SERVICE), any()))
                .thenReturn(new Paging<>(1, 0, 1, List.of(bundle), List.of()));

        for (JsonNode json : List.<JsonNode>of(mapper.valueToTree(eosc.getService("openaire.argos")),
                mapper.valueToTree(eosc.getServices(new LinkedMultiValueMap<>()).getResults().getFirst()))) {
            assertEquals(List.of("id", "service"), List.copyOf(json.propertyNames()));
            assertEquals("openaire.argos", json.get("id").asString());
            assertEquals("openaire.argos", json.get("service").get("id").asString());
            assertEquals("Argos", json.get("service").get("name").asString());
        }
    }

    @Test
    void aProviderIsReadThroughTheLot1ClassWithoutItsUsers() {
        Map<String, Object> provider = new LinkedHashMap<>();
        provider.put("id", "openaire");
        provider.put("name", "OpenAIRE");
        provider.put("abbreviation", "OpenAIRE");
        provider.put("users", List.of(Map.of("email", "owner@example.org", "name", "O", "surname", "U")));
        provider.put("unknown", "dropped");

        Provider lot1 = controller.toProvider(Map.of("provider", provider), null);
        JsonNode json = mapper.valueToTree(lot1);

        assertEquals("OpenAIRE", lot1.getAbbreviation());
        assertTrue(json.get("users") == null || json.get("users").isNull());
        assertFalse(json.has("unknown"));
    }

    @Test
    void theMainContactOfAProviderIsWrittenForThoseWhoManageItOnly() {
        Map<String, Object> managed = provider("managed");
        Map<String, Object> other = provider("other");
        when(access.canManage(auth, ResourceKind.PROVIDER, "managed")).thenReturn(true);

        assertEquals("main@example.org", controller.toProvider(managed, auth).getMainContact().getEmail());
        assertNull(controller.toProvider(other, auth).getMainContact());
        for (Provider provider : List.of(controller.toProvider(managed, null), controller.toProvider(other, null))) {
            assertNull(provider.getMainContact());
            assertNull(provider.getUsers());
            assertEquals("public@example.org", provider.getPublicContacts().getFirst().getEmail());
        }
    }

    @Test
    void providersAndDatasourcesAreWrittenWithTheirIdAtTheTopLevel() {
        JsonNode provider = mapper.valueToTree(EoscProvider.of(controller.toProvider(provider("openaire"), null)));
        JsonNode datasource = mapper.valueToTree(
                EoscDatasource.of(controller.toDatasource(datasource("openaire.zenodo", true, "node"))));

        assertEquals(List.of("id", "provider"), List.copyOf(provider.propertyNames()));
        assertEquals("openaire", provider.get("id").asString());
        assertEquals("openaire", provider.get("provider").get("id").asString());
        assertEquals(List.of("id", "datasource"), List.copyOf(datasource.propertyNames()));
        assertEquals("openaire.zenodo", datasource.get("id").asString());
        assertEquals("openaire.zenodo", datasource.get("datasource").get("serviceId").asString());
    }

    private static Map<String, Object> provider(String id) {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("id", id);
        bundle.put("active", true);
        bundle.put("properties", Map.of("channels", List.of("node")));
        bundle.put("provider", Map.of("id", id, "name", id,
                "users", List.of(Map.of("email", "owner@example.org")),
                "mainContact", Map.of("firstName", "A", "email", "main@example.org"),
                "publicContacts", List.of(Map.of("email", "public@example.org"))));
        return bundle;
    }

    @Test
    void aDatasourceIsReadThroughTheLot1Class() {
        Map<String, Object> datasource = new LinkedHashMap<>();
        datasource.put("id", "openaire.zenodo");
        datasource.put("serviceId", "openaire.zenodo");
        datasource.put("harvestable", true);
        datasource.put("researchEntityTypes", List.of("research_entity_type-research_data"));
        datasource.put("unknown", "dropped");

        Datasource lot1 = controller.toDatasource(Map.of("datasource", datasource));
        JsonNode json = mapper.valueToTree(lot1);

        assertEquals("openaire.zenodo", lot1.getServiceId());
        assertEquals(true, lot1.getHarvestable());
        assertEquals("research_entity_type-research_data", json.get("researchEntityTypes").get(0).asString());
        assertFalse(json.has("unknown"));
    }

    @Test
    void onlyActiveRecordsOnTheConfiguredChannelAreShown() {
        BundleService bundles = mock(BundleService.class);
        OpenAIREProperties properties = new OpenAIREProperties();
        properties.setEoscChannel("eosc");
        EoscController eosc = new EoscController(bundles, mock(ResourceTypeService.class), access, mapper,
                properties);
        when(bundles.search(any(), any())).thenReturn(new Paging<>(0, 0, 0, List.of(), List.of()));
        when(bundles.get(ResourceKind.SERVICE, "on.node")).thenReturn(service("on.node", true, "node"));
        when(bundles.get(ResourceKind.SERVICE, "inactive")).thenReturn(service("inactive", false, "eosc"));
        when(bundles.get(ResourceKind.SERVICE, "on.eosc")).thenReturn(service("on.eosc", true, "eosc"));

        eosc.getServices(new LinkedMultiValueMap<>(Map.of("channels", List.of("node"))));

        ArgumentCaptor<FacetFilter> filter = ArgumentCaptor.forClass(FacetFilter.class);
        verify(bundles).search(eq(ResourceKind.SERVICE), filter.capture());
        assertEquals("eosc", filter.getValue().getFilter().get("channels"));
        assertEquals(true, filter.getValue().getFilter().get("active"));
        assertThrows(ResourceNotFoundException.class, () -> eosc.getService("on.node"));
        assertThrows(ResourceNotFoundException.class, () -> eosc.getService("inactive"));
        assertEquals("on.eosc", eosc.getService("on.eosc").id());
    }

    @Test
    void onlyFacetsOnLot1FieldsAreShown() {
        BundleService bundles = mock(BundleService.class);
        ResourceTypeService resourceTypes = mock(ResourceTypeService.class);
        when(resourceTypes.getResourceType("service")).thenReturn(resourceType("service", Map.of(
                "tags", "$.service.tags[*]",
                "scientificSubdomain", "$.service.scientificDomains[*].scientificSubdomain",
                "mainContact_email", "$.service.mainContact.email",
                "oaOnly", "$.service.oaOnly[*]",
                "status", "$.status")));
        when(resourceTypes.getResourceType("provider")).thenReturn(resourceType("provider", Map.of(
                "abbreviation", "$.provider.abbreviation",
                "users_email", "$.provider.users[*].email")));
        when(bundles.search(eq(ResourceKind.SERVICE), any())).thenReturn(page(
                "tags", "scientificSubdomain", "mainContact_email", "oaOnly", "status"));
        EoscController eosc = new EoscController(bundles, resourceTypes, access, mapper, new OpenAIREProperties());

        assertEquals(List.of("tags", "scientificSubdomain", "mainContact_email"),
                fields(eosc.getServices(new LinkedMultiValueMap<>())));
        assertEquals(Set.of("abbreviation"), eosc.lot1IndexFields(ResourceKind.PROVIDER));
    }

    private static ResourceType resourceType(String name, Map<String, String> paths) {
        ResourceType type = new ResourceType();
        type.setName(name);
        type.setIndexFields(paths.entrySet().stream().map(entry -> {
            IndexField field = new IndexField();
            field.setName(entry.getKey());
            field.setPath(entry.getValue());
            return field;
        }).toList());
        return type;
    }

    private static Paging<Map<String, Object>> page(String... fields) {
        return new Paging<>(0, 0, 0, List.of(), java.util.Arrays.stream(fields)
                .map(field -> new Facet(field, field, List.of())).toList());
    }

    private static List<String> fields(Paging<?> page) {
        return page.getFacets().stream().map(Facet::getField).toList();
    }

    private static Map<String, Object> service(String id, boolean active, String channel) {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("id", id);
        bundle.put("active", active);
        bundle.put("properties", Map.of("channels", List.of(channel)));
        bundle.put("service", Map.of("id", id, "name", id));
        return bundle;
    }

    private static Map<String, Object> datasource(String id, boolean active, String channel) {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("id", id);
        bundle.put("active", active);
        bundle.put("properties", Map.of("channels", List.of(channel)));
        bundle.put("datasource", Map.of("id", id, "serviceId", id));
        return bundle;
    }
}
