package eu.openaire.catalogue.api;

import eu.openaire.catalogue.config.CatalogueProperties;
import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.invitations.InvitationService;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.ResourceKind;
import eu.openaire.catalogue.security.AccessRules;
import eu.openaire.catalogue.security.Roles;
import gr.uoa.di.madgik.catalogue.controller.GenericExceptionController;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the controllers inside a Spring context with method security on, so that their {@code @PreAuthorize} rules
 * apply. A request without a user carries the anonymous authentication a running application gives it.
 */
class RecordControllerTest {

    private static final String OWNER = "owner@example.org";
    private static final String STRANGER = "stranger@example.org";
    private static final String ADMIN = "admin@example.org";

    private static final BundleService bundles = mock(BundleService.class);
    private static final InvitationService invitations = mock(InvitationService.class);
    private static final OpenAIREProperties properties = new OpenAIREProperties();
    private static final CatalogueProperties reviewers = new CatalogueProperties();

    private final Roles roles = new Roles(reviewers);
    private AnnotationConfigApplicationContext context;
    private MockMvc mvc;

    @Configuration
    @EnableMethodSecurity
    static class Controllers {

        @Bean
        AccessRules access() {
            return new AccessRules(bundles);
        }

        @Bean
        OaServiceController serviceController(AccessRules access) {
            return new OaServiceController(bundles, access);
        }

        @Bean
        OaProviderController providerController(AccessRules access) {
            return new OaProviderController(bundles, access, invitations, properties);
        }

        @Bean
        NodeServiceController nodeServiceController(AccessRules access) {
            return new NodeServiceController(bundles, access);
        }

        @Bean
        NodeProviderController nodeProviderController(AccessRules access) {
            return new NodeProviderController(bundles, access, invitations, properties);
        }

        @Bean
        NodeDatasourceController nodeDatasourceController(AccessRules access) {
            return new NodeDatasourceController(bundles, access);
        }
    }

    @BeforeEach
    void setUp() {
        reset(bundles, invitations);
        reviewers.setAdmins(List.of(ADMIN));
        context = new AnnotationConfigApplicationContext(Controllers.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        context.getBean(OaServiceController.class),
                        context.getBean(OaProviderController.class),
                        context.getBean(NodeServiceController.class),
                        context.getBean(NodeProviderController.class),
                        context.getBean(NodeDatasourceController.class))
                .setControllerAdvice(new ErrorHandler(),
                        new GenericExceptionController(context.getBeanProvider(Tracer.class)))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key", "anonymous",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        when(bundles.get(any(), anyString())).thenAnswer(call -> {
            throw new ResourceNotFoundException(call.<String>getArgument(1), "record");
        });
        Map<String, Object> provider = bundle(ResourceKind.PROVIDER, "openaire", "approved", true,
                Map.of("name", "OpenAIRE", "users", List.of(Map.of("email", OWNER))));
        provider.put("properties", Map.of("channels", List.of("oa")));
        stub(ResourceKind.PROVIDER, provider);
        stub(ResourceKind.SERVICE, service("openaire.argos", "approved", true, List.of("oa")));
        stub(ResourceKind.SERVICE, service("openaire.graph", "approved", true, List.of("node")));
        stub(ResourceKind.SERVICE, service("openaire.openplato", "pending", false, List.of("oa")));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void anActiveServiceOnTheChannelIsPublic() throws Exception {
        mvc.perform(get("/oa/services/openaire.argos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("openaire.argos"));
    }

    @Test
    void aServiceOffTheChannelOrPendingIsHiddenFromThePublic() throws Exception {
        mvc.perform(get("/oa/services/openaire.graph")).andExpect(status().isNotFound());
        mvc.perform(get("/oa/services/openaire.openplato")).andExpect(status().isNotFound());
    }

    @Test
    void ownersAndReviewersReadPendingServices() throws Exception {
        mvc.perform(as(get("/oa/services/openaire.openplato"), OWNER)).andExpect(status().isOk());
        mvc.perform(as(get("/oa/services/openaire.openplato"), ADMIN)).andExpect(status().isOk());
        mvc.perform(as(get("/oa/services/openaire.openplato"), STRANGER)).andExpect(status().isNotFound());
    }

    @Test
    void publicListingsShowActiveServicesOnTheOpenAIREChannel() throws Exception {
        when(bundles.search(eq(ResourceKind.SERVICE), any())).thenReturn(new Paging<>(0, 0, 0, List.of(), List.of()));

        mvc.perform(get("/oa/services").param("active", "false").param("channels", "node"))
                .andExpect(status().isOk());

        ArgumentCaptor<FacetFilter> filter = ArgumentCaptor.forClass(FacetFilter.class);
        verify(bundles).search(eq(ResourceKind.SERVICE), filter.capture());
        assertEquals(true, filter.getValue().getFilter().get("active"));
        assertEquals("oa", filter.getValue().getFilter().get("channels"));
    }

    @Test
    void eachApiShowsTheServicesOnItsOwnChannel() throws Exception {
        mvc.perform(get("/node/services/openaire.graph")).andExpect(status().isOk());
        mvc.perform(get("/node/services/openaire.argos")).andExpect(status().isNotFound());
        mvc.perform(get("/oa/services/openaire.graph")).andExpect(status().isNotFound());
    }

    @Test
    void thoseWhoManageARecordReadItOnEveryApi() throws Exception {
        mvc.perform(as(get("/oa/services/openaire.graph"), OWNER)).andExpect(status().isOk());
        mvc.perform(as(get("/node/providers/openaire"), OWNER)).andExpect(status().isOk());
        mvc.perform(as(get("/node/services/openaire.argos"), STRANGER)).andExpect(status().isNotFound());
    }

    @Test
    void providersAreChannelScopedToo() throws Exception {
        mvc.perform(get("/oa/providers/openaire")).andExpect(status().isOk());
        mvc.perform(get("/node/providers/openaire")).andExpect(status().isNotFound());
        mvc.perform(get("/node/organisations/openaire")).andExpect(status().isNotFound());
    }

    @Test
    void nodeListingsShowActiveRecordsOnTheNodeChannel() throws Exception {
        when(bundles.search(any(), any())).thenReturn(new Paging<>(0, 0, 0, List.of(), List.of()));

        mvc.perform(get("/node/datasources").param("channels", "oa")).andExpect(status().isOk());
        mvc.perform(get("/node/providers")).andExpect(status().isOk());

        ArgumentCaptor<FacetFilter> filter = ArgumentCaptor.forClass(FacetFilter.class);
        verify(bundles).search(eq(ResourceKind.DATASOURCE), filter.capture());
        verify(bundles).search(eq(ResourceKind.PROVIDER), filter.capture());
        for (FacetFilter captured : filter.getAllValues()) {
            assertEquals(true, captured.getFilter().get("active"));
            assertEquals("node", captured.getFilter().get("channels"));
        }
    }

    @Test
    void aRecordIsAddedToTheChannelOfItsApi() throws Exception {
        when(bundles.create(eq(ResourceKind.SERVICE), any(), eq("node"), any()))
                .thenReturn(service("openaire.new", "pending", false, List.of("node")));

        mvc.perform(as(post("/node/services"), OWNER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"resourceOrganisation\":\"openaire\"}"))
                .andExpect(status().isCreated());
        verify(bundles).create(eq(ResourceKind.SERVICE), any(), eq("node"), any(UserInfo.class));
    }

    @Test
    void onlyReviewersChangeChannels() throws Exception {
        when(bundles.setChannel(eq(ResourceKind.DATASOURCE), eq("openaire.zenodo"), eq("oa"), anyBoolean(), any()))
                .thenReturn(List.of("node", "oa"));

        mvc.perform(put("/node/datasources/bundles/openaire.zenodo/channels/oa")).andExpect(status().isUnauthorized());
        mvc.perform(as(put("/node/datasources/bundles/openaire.zenodo/channels/oa"), OWNER))
                .andExpect(status().isForbidden());
        mvc.perform(as(put("/node/datasources/bundles/openaire.zenodo/channels/oa"), ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1]").value("oa"));
        verify(bundles).setChannel(eq(ResourceKind.DATASOURCE), eq("openaire.zenodo"), eq("oa"), eq(true),
                any(UserInfo.class));

        mvc.perform(as(delete("/node/datasources/bundles/openaire.zenodo/channels/oa"), OWNER))
                .andExpect(status().isForbidden());
        mvc.perform(as(delete("/node/datasources/bundles/openaire.zenodo/channels/oa"), ADMIN))
                .andExpect(status().isOk());
        verify(bundles).setChannel(eq(ResourceKind.DATASOURCE), eq("openaire.zenodo"), eq("oa"), eq(false),
                any(UserInfo.class));
    }

    @Test
    void reviewActionsTakeAnOptionalCommentInTheBody() throws Exception {
        Map<String, Object> bundle = service("openaire.argos", "approved", true, List.of("oa"));
        when(bundles.reject(eq(ResourceKind.SERVICE), eq("openaire.argos"), any(), any())).thenReturn(bundle);
        when(bundles.setActive(eq(ResourceKind.SERVICE), eq("openaire.argos"), anyBoolean(), any(), any()))
                .thenReturn(bundle);

        mvc.perform(as(post("/oa/services/bundles/openaire.argos/reject"), ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comment\":\"Missing contacts\"}"))
                .andExpect(status().isOk());
        verify(bundles).reject(eq(ResourceKind.SERVICE), eq("openaire.argos"), eq("Missing contacts"),
                any(UserInfo.class));

        mvc.perform(as(post("/oa/services/bundles/openaire.argos/deactivate"), ADMIN)).andExpect(status().isOk());
        verify(bundles).setActive(eq(ResourceKind.SERVICE), eq("openaire.argos"), eq(false), eq(null),
                any(UserInfo.class));
        mvc.perform(as(post("/oa/services/bundles/openaire.argos/activate"), OWNER)).andExpect(status().isForbidden());
    }

    @Test
    void providerUsersAreShownToThoseWhoManageTheProviderOnly() throws Exception {
        mvc.perform(get("/oa/providers/openaire"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").doesNotExist());
        mvc.perform(as(get("/oa/providers/openaire"), OWNER))
                .andExpect(jsonPath("$.users[0].email").value(OWNER));
    }

    @Test
    void addingNeedsASignedInUser() throws Exception {
        mvc.perform(post("/oa/services").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"resourceOrganisation\":\"openaire\"}"))
                .andExpect(status().isUnauthorized());
        verify(bundles, never()).create(any(), any(), any(), any());
    }

    @Test
    void providerUsersAddServicesToTheirProviderOnly() throws Exception {
        when(bundles.create(eq(ResourceKind.SERVICE), any(), eq("oa"), any()))
                .thenReturn(service("openaire.new", "pending", false, List.of("oa")));

        mvc.perform(as(post("/oa/services"), OWNER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"resourceOrganisation\":\"openaire\"}"))
                .andExpect(status().isCreated());
        mvc.perform(as(post("/oa/services"), STRANGER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"resourceOrganisation\":\"openaire\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyReviewersApprove() throws Exception {
        when(bundles.approve(eq(ResourceKind.SERVICE), eq("openaire.openplato"), any(), any()))
                .thenReturn(service("openaire.openplato", "approved", true, List.of("oa")));

        mvc.perform(post("/oa/services/bundles/openaire.openplato/approve")).andExpect(status().isUnauthorized());
        mvc.perform(as(post("/oa/services/bundles/openaire.openplato/approve"), OWNER)).andExpect(status().isForbidden());
        mvc.perform(as(post("/oa/services/bundles/openaire.openplato/approve"), ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));
        verify(bundles).approve(eq(ResourceKind.SERVICE), eq("openaire.openplato"), any(), any(UserInfo.class));
    }

    @Test
    void registeringAProviderNeedsAnInvitationOutsideTheReviewers() throws Exception {
        when(bundles.create(eq(ResourceKind.PROVIDER), any(), any(), any()))
                .thenReturn(bundle(ResourceKind.PROVIDER, "new", "pending", false, Map.of("name", "New")));
        String body = "{\"name\":\"New\",\"abbreviation\":\"new\"}";

        mvc.perform(as(post("/oa/providers"), STRANGER).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(as(post("/oa/providers"), STRANGER).param("invitation", "token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        verify(invitations).verify("token", STRANGER);
        mvc.perform(as(post("/oa/providers"), ADMIN).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void bundlesAndDeletionNeedAManager() throws Exception {
        mvc.perform(get("/oa/services/bundles/openaire.argos")).andExpect(status().isUnauthorized());
        mvc.perform(as(get("/oa/services/bundles/openaire.argos"), STRANGER)).andExpect(status().isForbidden());
        mvc.perform(as(get("/oa/services/bundles/openaire.argos"), OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));
        mvc.perform(as(delete("/oa/services/openaire.argos"), STRANGER)).andExpect(status().isForbidden());
        mvc.perform(as(get("/oa/services/bundles"), OWNER)).andExpect(status().isForbidden());
        mvc.perform(as(get("/oa/services/byProvider/openaire"), STRANGER)).andExpect(status().isForbidden());
    }

    @Test
    void anUpdateWhosePayloadIdDiffersFromThePathIsRefused() throws Exception {
        mvc.perform(as(put("/oa/services/openaire.argos"), OWNER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"openaire.other\",\"name\":\"Argos\"}"))
                .andExpect(status().isBadRequest());
        verify(bundles, never()).update(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void anUpdateTakesTheIdFromThePath() throws Exception {
        when(bundles.update(eq(ResourceKind.SERVICE), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(service("openaire.argos", "approved", true, List.of("oa")));

        mvc.perform(as(put("/oa/services/openaire.argos"), OWNER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Argos\",\"resourceOrganisation\":\"openaire\"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.captor();
        verify(bundles).update(eq(ResourceKind.SERVICE), payload.capture(), any(), any(), eq(false), any());
        assertEquals("openaire.argos", payload.getValue().get("id"));
    }

    @Test
    void anUpdateOfARecordTheUserDoesNotManageIsForbidden() throws Exception {
        mvc.perform(as(put("/oa/services/openaire.argos"), STRANGER).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Argos\"}"))
                .andExpect(status().isForbidden());
        verify(bundles, never()).update(any(), any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void anUpdateCarriesTheIfMatchVersionAndReturnsTheNewOne() throws Exception {
        Map<String, Object> stored = service("openaire.argos", "approved", true, List.of("oa"));
        stored.put("metadata", Map.of("modifiedAt", "200"));
        when(bundles.update(eq(ResourceKind.SERVICE), any(), any(), any(), anyBoolean(), any())).thenReturn(stored);

        mvc.perform(as(put("/oa/services/openaire.argos"), OWNER).contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"100\"")
                        .content("{\"id\":\"openaire.argos\",\"name\":\"Argos\",\"resourceOrganisation\":\"openaire\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"200\""));
        verify(bundles).update(eq(ResourceKind.SERVICE), any(), any(), any(), eq(false), eq("100"));
    }

    @Test
    void aUserWithoutAnEmailIsNotSignedIn() throws Exception {
        DefaultOidcUser user = new DefaultOidcUser(AuthorityUtils.createAuthorityList("ROLE_USER"),
                new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "no-email")));
        Authentication auth = new OAuth2AuthenticationToken(user, user.getAuthorities(), "test");
        SecurityContextHolder.getContext().setAuthentication(auth);

        mvc.perform(post("/oa/providers").principal(auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Anonymous\"}"))
                .andExpect(status().isForbidden());
        verify(bundles, never()).create(any(), any(), any(), any());
    }

    private void stub(ResourceKind kind, Map<String, Object> bundle) {
        doAnswer(call -> new LinkedHashMap<>(bundle)).when(bundles).get(kind, (String) bundle.get("id"));
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, String email) {
        DefaultOidcUser user = new DefaultOidcUser(roles.authorities(email), new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(60), Map.of("sub", email, "email", email, "given_name", "Test",
                "family_name", "User")));
        Authentication auth = new OAuth2AuthenticationToken(user, user.getAuthorities(), "test");
        SecurityContextHolder.getContext().setAuthentication(auth);
        return request.principal(auth);
    }

    private static Map<String, Object> service(String id, String status, boolean active, List<String> channels) {
        Map<String, Object> bundle = bundle(ResourceKind.SERVICE, id, status, active,
                Map.of("name", id, "resourceOrganisation", "openaire"));
        bundle.put("properties", Map.of("channels", channels));
        return bundle;
    }

    private static Map<String, Object> bundle(ResourceKind kind, String id, String status, boolean active,
                                              Map<String, Object> fields) {
        Map<String, Object> payload = new LinkedHashMap<>(fields);
        payload.put("id", id);
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put(kind.type(), payload);
        bundle.put("id", id);
        bundle.put("status", status);
        bundle.put("active", active);
        return bundle;
    }
}
