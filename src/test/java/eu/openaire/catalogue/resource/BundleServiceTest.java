package eu.openaire.catalogue.resource;

import eu.openaire.catalogue.config.OpenAIREProperties;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceAlreadyExistsException;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BundleServiceTest {

    private static final UserInfo OWNER = new UserInfo("sub", "owner@example.org", "Owner", "User",
            List.of("ROLE_USER"));
    private static final UserInfo REVIEWER = new UserInfo("sub2", "admin@example.org", "Admin", "User",
            List.of("ADMIN", "ROLE_ADMIN", "ROLE_USER"));

    private final Map<String, Map<String, Map<String, Object>>> store = new HashMap<>();
    private final List<BundleEvent> events = new ArrayList<>();
    private BundleService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        GenericResourceService resources = mock(GenericResourceService.class);
        when(resources.get(anyString(), anyString())).thenAnswer(call -> {
            Map<String, Object> bundle = table(call.getArgument(0)).get(call.<String>getArgument(1));
            if (bundle == null) {
                throw new ResourceNotFoundException(call.<String>getArgument(1), call.<String>getArgument(0));
            }
            return copy(bundle);
        });
        when(resources.add(anyString(), any(), anyBoolean())).thenAnswer(call -> {
            Map<String, Object> bundle = call.getArgument(1);
            table(call.getArgument(0)).put(Bundles.id(bundle), copy(bundle));
            return bundle;
        });
        when(resources.update(anyString(), any(), anyBoolean())).thenAnswer(call -> {
            Map<String, Object> bundle = call.getArgument(1);
            table(call.getArgument(0)).put(Bundles.id(bundle), copy(bundle));
            return bundle;
        });
        when(resources.delete(anyString(), anyString())).thenAnswer(call ->
                table(call.getArgument(0)).remove(call.<String>getArgument(1)));
        when(resources.getResults(any(FacetFilter.class))).thenAnswer(call -> {
            FacetFilter filter = call.getArgument(0);
            List<Map<String, Object>> results = table(filter.getResourceType()).values().stream()
                    .filter(bundle -> filter.getFilter().entrySet().stream().allMatch(e -> matches(bundle, filter, e)))
                    .map(BundleServiceTest::copy)
                    .toList();
            return new Paging<>(results.size(), 0, results.size(), results, List.of());
        });
        ApplicationEventPublisher publisher = event -> events.add((BundleEvent) event);
        service = new BundleService(resources, publisher, new OpenAIREProperties(), JsonMapper.builder().build());
    }

    @Test
    void aNewProviderIsPendingInactiveAndManagedByItsRegistrant() {
        Map<String, Object> bundle = service.create(ResourceKind.PROVIDER,
                payload("name", "Athena Research Center", "abbreviation", "ATHENA RC"), "oa", OWNER);

        assertEquals("athena_rc", Bundles.id(bundle));
        assertEquals("pending", Bundles.status(bundle));
        assertFalse(Bundles.active(bundle));
        Map<String, Object> provider = Bundles.payload(bundle, ResourceKind.PROVIDER);
        assertEquals("athena_rc", provider.get("id"));
        assertEquals("openaire", provider.get("catalogueId"));
        assertEquals(List.of("owner@example.org"), Bundles.userEmails(provider));
        Map<?, ?> entry = (Map<?, ?>) ((List<?>) bundle.get("loggingInfo")).getFirst();
        assertEquals("onboard", entry.get("type"));
        assertEquals("registered", entry.get("actionType"));
        assertEquals(entry, bundle.get("latestOnboardingInfo"));
        assertEquals(BundleEvent.Action.CREATED, events.getLast().action());
    }

    @Test
    void entriesAndMetadataFollowTheLot1Shape() {
        UserInfo someone = new UserInfo("sub3", "someone@example.org", "Some", "One", List.of("ROLE_USER"));
        Map<String, Object> bundle = service.create(ResourceKind.PROVIDER,
                payload("name", "Athena Research Center", "abbreviation", "athenarc"), "oa", someone);

        Map<?, ?> registered = (Map<?, ?>) bundle.get("latestOnboardingInfo");
        assertEquals("someone@example.org", registered.get("userEmail"));
        assertEquals("Some One", registered.get("userFullName"));
        assertEquals("USER", registered.get("userRole"));
        assertEquals(java.util.Set.of("date", "userEmail", "userFullName", "userRole", "type", "comment", "actionType"),
                registered.keySet());
        Map<?, ?> metadata = (Map<?, ?>) bundle.get("metadata");
        assertEquals(java.util.Set.of("registeredBy", "registeredAt", "modifiedBy", "modifiedAt", "terms", "published"),
                metadata.keySet());
        assertEquals("Some One", metadata.get("registeredBy"));
        assertEquals(metadata.get("registeredAt"), metadata.get("modifiedAt"));
        assertEquals(List.of("someone@example.org"), metadata.get("terms"));
        assertEquals(false, metadata.get("published"));
        assertEquals(Map.of("id", "sub3", "email", "someone@example.org", "name", "Some", "surname", "One"),
                ((List<?>) Bundles.payload(bundle, ResourceKind.PROVIDER).get("users")).getFirst());

        Map<String, Object> approved = service.approve(ResourceKind.PROVIDER, "athenarc", "ok", REVIEWER);

        Map<?, ?> approval = (Map<?, ?>) approved.get("latestOnboardingInfo");
        assertEquals("approved", approval.get("actionType"));
        assertEquals("ADMIN", approval.get("userRole"));
        assertEquals("ok", approval.get("comment"));
        Map<?, ?> updatedMetadata = (Map<?, ?>) approved.get("metadata");
        assertEquals("Some One", updatedMetadata.get("registeredBy"));
        assertEquals(metadata.get("registeredAt"), updatedMetadata.get("registeredAt"));
        assertEquals("Admin User", updatedMetadata.get("modifiedBy"));
        assertEquals(List.of("someone@example.org"), updatedMetadata.get("terms"));
        assertEquals(2, ((List<?>) approved.get("loggingInfo")).size());
    }

    @Test
    void aNewServiceGetsTheLegacyIdAndTheChannelItWasAddedThrough() {
        provider("athenarc");
        Map<String, Object> bundle = service.create(ResourceKind.SERVICE,
                payload("name", "BIP! Scholar", "resourceOrganisation", "athenarc"), "oa", OWNER);

        assertEquals("athenarc.bip_scholar", Bundles.id(bundle));
        assertEquals("pending", Bundles.status(bundle));
        assertEquals(List.of("oa"), Channels.of(bundle));
    }

    @Test
    void aServiceNeedsAnExistingProvider() {
        assertThrows(ResourceException.class, () -> service.create(ResourceKind.SERVICE,
                payload("name", "Orphan", "resourceOrganisation", "missing"), "oa", OWNER));
    }

    @Test
    void aDuplicateIdIsRefused() {
        provider("athenarc");
        assertThrows(ResourceAlreadyExistsException.class, () -> service.create(ResourceKind.PROVIDER,
                payload("name", "Other", "abbreviation", "athenarc"), "oa", OWNER));
    }

    @Test
    void approvalActivatesAndRejectionDeactivates() {
        provider("athenarc");

        Map<String, Object> approved = service.approve(ResourceKind.PROVIDER, "athenarc", "fine", REVIEWER);
        assertEquals("approved", Bundles.status(approved));
        assertTrue(Bundles.active(approved));
        assertEquals("approved", ((Map<?, ?>) approved.get("latestOnboardingInfo")).get("actionType"));

        Map<String, Object> rejected = service.reject(ResourceKind.PROVIDER, "athenarc", null, REVIEWER);
        assertEquals("rejected", Bundles.status(rejected));
        assertFalse(Bundles.active(rejected));
    }

    @Test
    void onlyAnApprovedRecordCanBeActivated() {
        provider("athenarc");
        assertThrows(ResourceException.class, () -> service.setActive(ResourceKind.PROVIDER, "athenarc", true, null, REVIEWER));

        service.approve(ResourceKind.PROVIDER, "athenarc", null, REVIEWER);
        assertFalse(Bundles.active(service.setActive(ResourceKind.PROVIDER, "athenarc", false, null, REVIEWER)));
        assertTrue(Bundles.active(service.setActive(ResourceKind.PROVIDER, "athenarc", true, null, REVIEWER)));
    }

    @Test
    void anUpdateKeepsStatusAndActivity() {
        provider("athenarc");
        service.approve(ResourceKind.PROVIDER, "athenarc", null, REVIEWER);

        Map<String, Object> updated = service.update(ResourceKind.PROVIDER,
                payload("id", "athenarc", "name", "Renamed", "abbreviation", "athenarc"), "rename", OWNER, false, null);

        assertEquals("approved", Bundles.status(updated));
        assertTrue(Bundles.active(updated));
        assertEquals("Renamed", Bundles.payload(updated, ResourceKind.PROVIDER).get("name"));
        assertEquals("rename", ((Map<?, ?>) updated.get("latestUpdateInfo")).get("comment"));
    }

    @Test
    void anUpdatedRejectedRecordIsPendingAgain() {
        provider("athenarc");
        service.reject(ResourceKind.PROVIDER, "athenarc", "incomplete", REVIEWER);

        Map<String, Object> updated = service.update(ResourceKind.PROVIDER,
                payload("id", "athenarc", "name", "Fixed", "abbreviation", "athenarc"), "fixed", OWNER, false, null);

        assertEquals("pending", Bundles.status(updated));
        assertFalse(Bundles.active(updated));
        assertEquals(BundleEvent.Action.RESUBMITTED, events.getLast().action());
    }

    @Test
    void anUpdatedPendingOrDeactivatedRecordKeepsItsStatus() {
        provider("athenarc");
        Map<String, Object> pending = service.update(ResourceKind.PROVIDER,
                payload("id", "athenarc", "name", "Renamed", "abbreviation", "athenarc"), null, OWNER, false, null);
        assertEquals("pending", Bundles.status(pending));
        assertFalse(Bundles.active(pending));
        assertEquals(BundleEvent.Action.UPDATED, events.getLast().action());

        service.approve(ResourceKind.PROVIDER, "athenarc", null, REVIEWER);
        service.setActive(ResourceKind.PROVIDER, "athenarc", false, null, REVIEWER);
        Map<String, Object> deactivated = service.update(ResourceKind.PROVIDER,
                payload("id", "athenarc", "name", "Again", "abbreviation", "athenarc"), null, OWNER, false, null);
        assertEquals("approved", Bundles.status(deactivated));
        assertFalse(Bundles.active(deactivated));
    }

    @Test
    void onlyAReviewerMovesAServiceToAnotherProvider() {
        provider("athenarc");
        provider("openaire");
        service.create(ResourceKind.SERVICE, payload("name", "Amnesia", "resourceOrganisation", "athenarc"), "oa", OWNER);
        Map<String, Object> moved = payload("id", "athenarc.amnesia", "name", "Amnesia", "resourceOrganisation", "openaire");

        assertThrows(ResourceException.class, () -> service.update(ResourceKind.SERVICE, moved, null, OWNER, false, null));
        assertEquals("openaire", Bundles.payload(service.update(ResourceKind.SERVICE, moved, null, REVIEWER, true, null),
                ResourceKind.SERVICE).get("resourceOrganisation"));
    }

    @Test
    void aMoveIsLoggedAsMoved() {
        provider("athenarc");
        provider("openaire");
        service.create(ResourceKind.SERVICE, payload("name", "Amnesia", "resourceOrganisation", "athenarc"), "oa", OWNER);

        Map<String, Object> moved = service.update(ResourceKind.SERVICE,
                payload("id", "athenarc.amnesia", "name", "Amnesia", "resourceOrganisation", "openaire"),
                "merged providers", REVIEWER, true, null);

        Map<?, ?> entry = (Map<?, ?>) ((List<?>) moved.get("loggingInfo")).getFirst();
        assertEquals("update", entry.get("type"));
        assertEquals("moved", entry.get("actionType"));
        assertEquals("Moved from 'athenarc' to 'openaire': merged providers", entry.get("comment"));
        assertEquals(entry, moved.get("latestUpdateInfo"));
    }

    @Test
    void anUnchangedUpdateIsNotStoredOrLogged() {
        provider("athenarc");
        Map<String, Object> before = service.get(ResourceKind.PROVIDER, "athenarc");
        int published = events.size();

        Map<String, Object> after = service.update(ResourceKind.PROVIDER,
                Bundles.payload(before, ResourceKind.PROVIDER), "nothing", OWNER, false, null);

        assertEquals(before, after);
        assertEquals(published, events.size());
    }

    @Test
    void repeatedTransitionsAreNotLogged() {
        provider("athenarc");
        service.approve(ResourceKind.PROVIDER, "athenarc", null, REVIEWER);
        int entries = loggingInfo("athenarc").size();
        int published = events.size();

        service.approve(ResourceKind.PROVIDER, "athenarc", null, REVIEWER);
        service.setActive(ResourceKind.PROVIDER, "athenarc", true, null, REVIEWER);
        assertEquals(entries, loggingInfo("athenarc").size());

        service.reject(ResourceKind.PROVIDER, "athenarc", null, REVIEWER);
        service.reject(ResourceKind.PROVIDER, "athenarc", null, REVIEWER);
        service.setActive(ResourceKind.PROVIDER, "athenarc", false, null, REVIEWER);
        assertEquals(entries + 1, loggingInfo("athenarc").size());
        assertEquals(published + 1, events.size());
    }

    @Test
    void approvingADeactivatedRecordActivatesIt() {
        provider("athenarc");
        service.approve(ResourceKind.PROVIDER, "athenarc", null, REVIEWER);
        service.setActive(ResourceKind.PROVIDER, "athenarc", false, null, REVIEWER);

        assertTrue(Bundles.active(service.approve(ResourceKind.PROVIDER, "athenarc", null, REVIEWER)));
    }

    @Test
    void aProviderWithServicesCannotBeDeleted() {
        provider("athenarc");
        service.create(ResourceKind.SERVICE, payload("name", "Amnesia", "resourceOrganisation", "athenarc"), "oa", OWNER);

        assertThrows(ResourceException.class, () -> service.delete(ResourceKind.PROVIDER, "athenarc", REVIEWER));
        service.delete(ResourceKind.SERVICE, "athenarc.amnesia", REVIEWER);
        service.delete(ResourceKind.PROVIDER, "athenarc", REVIEWER);
        assertFalse(service.exists(ResourceKind.PROVIDER, "athenarc"));
    }

    @Test
    void channelsChangeOneAtATime() {
        provider("openaire");
        service.create(ResourceKind.SERVICE, payload("name", "Argos", "resourceOrganisation", "openaire"), "oa", OWNER);

        assertEquals(List.of("oa", "node"),
                service.setChannel(ResourceKind.SERVICE, "openaire.argos", "node", true, REVIEWER));
        assertEquals(List.of("node"),
                service.setChannel(ResourceKind.SERVICE, "openaire.argos", "oa", false, REVIEWER));
    }

    @Test
    void everyRecordTypeHasItsOwnChannels() {
        Map<String, Object> provider = service.create(ResourceKind.PROVIDER,
                payload("name", "OpenAIRE", "abbreviation", "openaire"), "node", OWNER);
        service.create(ResourceKind.SERVICE, payload("name", "Zenodo", "resourceOrganisation", "openaire"), "oa", OWNER);
        Map<String, Object> datasource = service.create(ResourceKind.DATASOURCE,
                payload("serviceId", "openaire.zenodo"), "node", OWNER);

        assertEquals(List.of("node"), Channels.of(provider));
        assertEquals(List.of("node"), Channels.of(datasource));
        assertEquals(List.of("node", "oa"),
                service.setChannel(ResourceKind.PROVIDER, "openaire", "oa", true, REVIEWER));
        assertEquals(List.of("oa"), Channels.of(service.get(ResourceKind.SERVICE, "openaire.zenodo")));
    }

    @Test
    void anUnknownChannelIsRefused() {
        provider("openaire");
        assertThrows(ResourceException.class,
                () -> service.setChannel(ResourceKind.PROVIDER, "openaire", "eosc", true, REVIEWER));
    }

    @Test
    void anUpdateKeepsTheChannels() {
        provider("openaire");
        service.setChannel(ResourceKind.PROVIDER, "openaire", "node", true, REVIEWER);
        Map<String, Object> payload = new LinkedHashMap<>(
                Bundles.payload(service.get(ResourceKind.PROVIDER, "openaire"), ResourceKind.PROVIDER));
        payload.put("name", "OpenAIRE AMKE");

        Map<String, Object> updated = service.update(ResourceKind.PROVIDER, payload, null, OWNER, false, null);

        assertEquals("OpenAIRE AMKE", Bundles.payload(updated, ResourceKind.PROVIDER).get("name"));
        assertEquals(List.of("oa", "node"), Channels.of(updated));
    }

    @Test
    void anUpdateAgainstAnOlderVersionIsRefused() {
        provider("openaire");
        String version = Bundles.version(service.get(ResourceKind.PROVIDER, "openaire"));
        Map<String, Object> payload = payload("id", "openaire", "name", "OpenAIRE AMKE", "abbreviation", "openaire");

        ResourceException stale = assertThrows(ResourceException.class,
                () -> service.update(ResourceKind.PROVIDER, payload, null, OWNER, false, "older"));
        assertEquals(412, stale.getStatus().value());
        assertEquals("OpenAIRE AMKE", Bundles.payload(
                service.update(ResourceKind.PROVIDER, payload, null, OWNER, false, version), ResourceKind.PROVIDER)
                .get("name"));
    }

    @Test
    void aDatasourceCannotMoveEvenForAReviewer() {
        provider("openaire");
        service.create(ResourceKind.SERVICE, payload("name", "Zenodo", "resourceOrganisation", "openaire"), "oa", OWNER);
        service.create(ResourceKind.SERVICE, payload("name", "Argos", "resourceOrganisation", "openaire"), "oa", OWNER);
        service.create(ResourceKind.DATASOURCE, payload("serviceId", "openaire.zenodo"), "oa", OWNER);

        assertThrows(ResourceException.class, () -> service.update(ResourceKind.DATASOURCE,
                payload("id", "openaire.zenodo", "serviceId", "openaire.argos"), null, REVIEWER, true, null));
    }

    @Test
    void aServiceIsApprovedOnlyUnderAnApprovedActiveProvider() {
        provider("openaire");
        service.create(ResourceKind.SERVICE, payload("name", "Zenodo", "resourceOrganisation", "openaire"), "oa", OWNER);

        ResourceException pending = assertThrows(ResourceException.class,
                () -> service.approve(ResourceKind.SERVICE, "openaire.zenodo", null, REVIEWER));
        assertEquals(409, pending.getStatus().value());

        service.approve(ResourceKind.PROVIDER, "openaire", null, REVIEWER);
        assertEquals("approved", Bundles.status(service.approve(ResourceKind.SERVICE, "openaire.zenodo", null, REVIEWER)));
    }

    @Test
    void emptyExtrasAreDropped() {
        provider("openaire");
        Map<String, Object> created = service.create(ResourceKind.SERVICE,
                payload("name", "Zenodo", "resourceOrganisation", "openaire", "extras", Map.of()), "oa", OWNER);
        assertFalse(Bundles.payload(created, ResourceKind.SERVICE).containsKey("extras"));

        Map<String, Object> updated = service.update(ResourceKind.SERVICE, payload("id", "openaire.zenodo",
                "name", "Zenodo", "resourceOrganisation", "openaire", "extras", Map.of()), null, OWNER, false, null);
        assertFalse(Bundles.payload(updated, ResourceKind.SERVICE).containsKey("extras"));
    }

    @Test
    void anUnchangedChannelIsNotStoredOrLogged() {
        provider("openaire");
        int published = events.size();
        int logged = loggingInfo("openaire").size();

        assertEquals(List.of("oa"), service.setChannel(ResourceKind.PROVIDER, "openaire", "oa", true, REVIEWER));
        assertEquals(published, events.size());
        assertEquals(logged, loggingInfo("openaire").size());
    }

    private void provider(String abbreviation) {
        service.create(ResourceKind.PROVIDER, payload("name", abbreviation, "abbreviation", abbreviation), "oa", OWNER);
    }

    private List<?> loggingInfo(String providerId) {
        return (List<?>) service.get(ResourceKind.PROVIDER, providerId).get("loggingInfo");
    }

    private Map<String, Map<String, Object>> table(String type) {
        return store.computeIfAbsent(type, key -> new HashMap<>());
    }

    private static boolean matches(Map<String, Object> bundle, FacetFilter filter, Map.Entry<String, Object> entry) {
        Object value = switch (entry.getKey()) {
            case "resourceOrganisation" -> Bundles.payload(bundle, ResourceKind.of(filter.getResourceType()))
                    .get("resourceOrganisation");
            default -> bundle.get(entry.getKey());
        };
        return Objects.equals(String.valueOf(value), String.valueOf(entry.getValue()));
    }

    private static Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            payload.put((String) keyValues[i], keyValues[i + 1]);
        }
        return payload;
    }

    /**
     * Deep copy through the JSON shapes the store holds, so that stored bundles are not shared with callers.
     */
    @SuppressWarnings("unchecked")
    private static <T> T copy(T value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put((String) k, copy(v)));
            return (T) copy;
        }
        if (value instanceof List<?> list) {
            return (T) new ArrayList<>(list.stream().map(BundleServiceTest::copy).toList());
        }
        return value;
    }
}
