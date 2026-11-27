package eu.openaire.catalogue.security;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.ResourceKind;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks the view rule against the manage rule on stored records, and the read-only access of the users of a service's
 * resource providers.
 */
class AccessRulesTest {

    private static final String OWNER = "owner@example.org";
    private static final String PARTNER = "partner@example.org";
    private static final String STRANGER = "stranger@example.org";
    private static final String OA_ADMIN = "oa-admin@example.org";
    private static final String ADMIN = "admin@example.org";

    private final BundleService bundles = mock(BundleService.class);
    private final Map<String, Map<String, Object>> stored = new LinkedHashMap<>();
    private Roles roles;
    private AccessRules access;

    @BeforeEach
    void setUp() {
        OpenAIREProperties properties = new OpenAIREProperties();
        properties.getRoles().getAdmin().setEmails(List.of(ADMIN));
        OpenAIREProperties.RoleScope oaAdmins = new OpenAIREProperties.RoleScope();
        oaAdmins.setEmails(List.of(OA_ADMIN));
        properties.getRoles().setChannels(Map.of("oa", oaAdmins));
        roles = new Roles(properties);
        access = new AccessRules(bundles, roles);
        when(bundles.get(any(), anyString())).thenAnswer(call -> {
            Map<String, Object> bundle = stored.get(call.getArgument(0) + "/" + call.getArgument(1));
            if (bundle == null) {
                throw new ResourceNotFoundException(call.<String>getArgument(1), "record");
            }
            return bundle;
        });

        store(ResourceKind.PROVIDER, "openaire", List.of("oa"), Map.of("users", List.of(Map.of("email", OWNER))));
        store(ResourceKind.PROVIDER, "partner", List.of("node"), Map.of("users", List.of(Map.of("email", PARTNER))));
        store(ResourceKind.SERVICE, "openaire.argos", List.of("oa"), Map.of("resourceOrganisation", "openaire"));
        store(ResourceKind.SERVICE, "openaire.graph", List.of("node"), Map.of("resourceOrganisation", "openaire",
                "resourceProviders", List.of("partner")));
        store(ResourceKind.SERVICE, "openaire.single", List.of("node"), Map.of("resourceOrganisation", "openaire",
                "resourceProviders", "partner"));
        store(ResourceKind.DATASOURCE, "openaire.graph.ds", List.of("node"), Map.of("serviceId", "openaire.graph"));
    }

    @Test
    void viewingAndManagingAgreeForEveryoneButTheUsersOfResourceProviders() {
        List<String[]> records = List.of(new String[]{"PROVIDER", "openaire"}, new String[]{"PROVIDER", "partner"},
                new String[]{"SERVICE", "openaire.argos"}, new String[]{"SERVICE", "openaire.graph"},
                new String[]{"DATASOURCE", "openaire.graph.ds"});
        for (String email : List.of(OWNER, STRANGER, OA_ADMIN, ADMIN)) {
            Authentication auth = signedIn(email);
            for (String[] record : records) {
                ResourceKind kind = ResourceKind.valueOf(record[0]);
                assertEquals(access.canManage(auth, kind, record[1]),
                        access.canViewBundle(auth, kind, stored.get(kind + "/" + record[1])), email + " " + record[1]);
            }
        }
    }

    @Test
    void theUsersOfAResourceProviderViewTheServiceButNotItsDatasources() {
        Authentication partner = signedIn(PARTNER);

        assertTrue(access.canView(partner, ResourceKind.SERVICE, "openaire.graph"));
        assertTrue(access.canView(partner, ResourceKind.SERVICE, "openaire.single"));
        assertFalse(access.canManage(partner, ResourceKind.SERVICE, "openaire.graph"));
        assertFalse(access.canView(partner, ResourceKind.SERVICE, "openaire.argos"));
        assertFalse(access.canView(partner, ResourceKind.DATASOURCE, "openaire.graph.ds"));
    }

    @Test
    void blankOwnersAndResourceProvidersMatchNoOneWithoutBeingRead() {
        store(ResourceKind.SERVICE, "blank", List.of("node"), Map.of("resourceOrganisation", " ",
                "resourceProviders", List.of("")));

        assertFalse(access.canView(signedIn(OWNER), ResourceKind.SERVICE, "blank"));
        assertFalse(access.managesAsUser(signedIn(OWNER), ResourceKind.PROVIDER, ""));
        verify(bundles, never()).get(ResourceKind.PROVIDER, "");
        verify(bundles, never()).get(ResourceKind.PROVIDER, " ");
    }

    @Test
    void aGlobalReviewerViewsAnyIdSoThatAMissingRecordIsReportedAsMissing() {
        assertTrue(access.canView(signedIn(ADMIN), ResourceKind.SERVICE, "missing"));
        assertFalse(access.canView(signedIn(OWNER), ResourceKind.SERVICE, "missing"));
    }

    @Test
    void managingAsAUserIgnoresChannelReview() {
        assertTrue(access.canManage(signedIn(OA_ADMIN), ResourceKind.PROVIDER, "openaire"));
        assertFalse(access.managesAsUser(signedIn(OA_ADMIN), ResourceKind.PROVIDER, "openaire"));
        assertTrue(access.managesAsUser(signedIn(OWNER), ResourceKind.PROVIDER, "openaire"));
    }

    private void store(ResourceKind kind, String id, List<String> channels, Map<String, Object> fields) {
        Map<String, Object> payload = new LinkedHashMap<>(fields);
        payload.put("id", id);
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put(kind.type(), payload);
        bundle.put("id", id);
        bundle.put("active", true);
        bundle.put("properties", Map.of("channels", channels));
        stored.put(kind + "/" + id, bundle);
    }

    private Authentication signedIn(String email) {
        DefaultOidcUser user = new DefaultOidcUser(roles.authorities(email), new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(60), Map.of("sub", email, "email", email, "given_name", "Test",
                "family_name", "User")));
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "test");
    }
}
