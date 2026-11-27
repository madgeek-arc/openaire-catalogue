package eu.openaire.catalogue.api;

import eu.openaire.catalogue.config.OpenAIREProperties;
import eu.openaire.catalogue.invitations.InvitationService;
import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.RecordGroups;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
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
    private static final String OA_ADMIN = "oa-admin@example.org";

    private static final BundleService bundles = mock(BundleService.class);
    private static final RecordGroups groups = mock(RecordGroups.class);
    private static final InvitationService invitations = mock(InvitationService.class);
    private static final OpenAIREProperties properties = new OpenAIREProperties();
    private static final Roles roles = new Roles(properties);
    private AnnotationConfigApplicationContext context;
    private MockMvc mvc;

    @Configuration
    @EnableMethodSecurity
    static class Controllers {

        @Bean
        AccessRules access() {
            return new AccessRules(bundles, roles);
        }

        @Bean
        OaServiceController serviceController(AccessRules access) {
            return new OaServiceController(bundles, groups, access);
        }

        @Bean
        OaProviderController providerController(AccessRules access) {
            return new OaProviderController(bundles, groups, access, invitations, properties);
        }

        @Bean
        NodeServiceController nodeServiceController(AccessRules access) {
            return new NodeServiceController(bundles, groups, access);
        }

        @Bean
        NodeProviderController nodeProviderController(AccessRules access) {
            return new NodeProviderController(bundles, groups, access, invitations, properties);
        }

        @Bean
        NodeDatasourceController nodeDatasourceController(AccessRules access) {
            return new NodeDatasourceController(bundles, groups, access);
        }
    }

    @BeforeEach
    void setUp() {
        reset(bundles, groups, invitations);
        properties.getRoles().getAdmin().setEmails(List.of(ADMIN));
        OpenAIREProperties.RoleScope oaAdmins = new OpenAIREProperties.RoleScope();
        oaAdmins.setEmails(List.of(OA_ADMIN));
        properties.getRoles().setChannels(Map.of("oa", oaAdmins));
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
        stub(ResourceKind.SERVICE, service("openaire.shared", "pending", false, List.of("oa", "node")));
        Map<String, Object> nodeProvider = bundle(ResourceKind.PROVIDER, "nodeprov", "approved", true,
                Map.of("name", "Node provider"));
        nodeProvider.put("properties", Map.of("channels", List.of("node")));
        stub(ResourceKind.PROVIDER, nodeProvider);
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
    void invalidPagingIsABadRequest() throws Exception {
        for (String path : List.of("/oa/services", "/oa/services/by/portfolios", "/node/datasources")) {
            mvc.perform(get(path).param("from", "abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Parameter 'from' must be a non-negative integer."));
            mvc.perform(get(path).param("quantity", "-1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Parameter 'quantity' must be a non-negative integer."));
        }
    }

    @Test
    void providersAndServicesAreListedByNameUnlessTheClientSetsAnOrder() throws Exception {
        when(bundles.search(any(), any())).thenReturn(new Paging<>(0, 0, 0, List.of(), List.of()));

        mvc.perform(get("/oa/providers")).andExpect(status().isOk());
        mvc.perform(get("/oa/services").param("sort", "abbreviation").param("order", "desc"))
                .andExpect(status().isOk());
        mvc.perform(get("/node/datasources")).andExpect(status().isOk());

        ArgumentCaptor<FacetFilter> provider = ArgumentCaptor.forClass(FacetFilter.class);
        ArgumentCaptor<FacetFilter> service = ArgumentCaptor.forClass(FacetFilter.class);
        ArgumentCaptor<FacetFilter> datasource = ArgumentCaptor.forClass(FacetFilter.class);
        verify(bundles).search(eq(ResourceKind.PROVIDER), provider.capture());
        verify(bundles).search(eq(ResourceKind.SERVICE), service.capture());
        verify(bundles).search(eq(ResourceKind.DATASOURCE), datasource.capture());
        assertEquals(Map.of("name", Map.of("order", "asc")), provider.getValue().getOrderBy());
        assertEquals(Map.of("abbreviation", Map.of("order", "desc")), service.getValue().getOrderBy());
        assertNull(datasource.getValue().getOrderBy());
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
    void summaryListingsShowSummariesOfActiveRecordsOnTheChannel() throws Exception {
        Map<String, Object> argos = service("openaire.argos", "approved", true, List.of("oa"));
        when(bundles.search(eq(ResourceKind.SERVICE), any()))
                .thenReturn(new Paging<>(1, 0, 1, List.of(argos), List.of()));
        stubSummaries(ResourceKind.SERVICE);

        mvc.perform(get("/oa/services").param("view", "SUMMARY").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id").value("openaire.argos"))
                .andExpect(jsonPath("$.results[0].resourceOrganisation").doesNotExist());
        mvc.perform(get("/oa/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].resourceOrganisation").value("openaire"));

        ArgumentCaptor<FacetFilter> filter = ArgumentCaptor.forClass(FacetFilter.class);
        verify(bundles, times(2)).search(eq(ResourceKind.SERVICE), filter.capture());
        for (FacetFilter captured : filter.getAllValues()) {
            assertEquals(Map.of("active", true, "channels", "oa"), captured.getFilter());
        }
    }

    @Test
    void groupedListingsShowActiveRecordsOnTheChannelInEitherView() throws Exception {
        Map<String, Object> argos = service("openaire.argos", "approved", true, List.of("oa"));
        when(groups.group(eq(ResourceKind.SERVICE), any(), eq("portfolios")))
                .thenReturn(Map.of("Research", List.of(argos)));
        stubSummaries(ResourceKind.SERVICE);

        mvc.perform(get("/oa/services/by/portfolios").param("active", "false").param("channels", "node"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Research[0].resourceOrganisation").value("openaire"))
                .andExpect(jsonPath("$.Research[0].status").doesNotExist());
        mvc.perform(get("/oa/services/by/portfolios").param("view", "summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Research[0].id").value("openaire.argos"))
                .andExpect(jsonPath("$.Research[0].name").value("openaire.argos"))
                .andExpect(jsonPath("$.Research[0].resourceOrganisation").doesNotExist());

        ArgumentCaptor<FacetFilter> filter = ArgumentCaptor.forClass(FacetFilter.class);
        verify(groups, times(2)).group(eq(ResourceKind.SERVICE), filter.capture(), eq("portfolios"));
        for (FacetFilter captured : filter.getAllValues()) {
            assertEquals(Map.of("active", true, "channels", "oa"), captured.getFilter());
        }
    }

    @Test
    void anUnknownViewIsRejected() throws Exception {
        mvc.perform(get("/oa/services").param("view", "compact")).andExpect(status().isBadRequest());
        mvc.perform(get("/oa/services/by/portfolios").param("view", "compact")).andExpect(status().isBadRequest());
        verify(bundles, never()).search(any(), any());
        verify(groups, never()).group(any(), any(), any());
    }

    @Test
    void providerSummariesHideTheirUsers() throws Exception {
        Map<String, Object> provider = bundle(ResourceKind.PROVIDER, "openaire", "approved", true,
                Map.of("name", "OpenAIRE", "users", List.of(Map.of("email", OWNER))));
        when(groups.group(eq(ResourceKind.PROVIDER), any(), eq("structureTypes")))
                .thenReturn(Map.of("Research", List.of(provider)));
        when(groups.snippets(ResourceKind.PROVIDER)).thenReturn(payload -> payload);

        for (String view : List.of("full", "summary")) {
            mvc.perform(get("/oa/providers/by/structureTypes").param("view", view))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.Research[0].name").value("OpenAIRE"))
                    .andExpect(jsonPath("$.Research[0].users").doesNotExist());
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
    void theMainContactIsShownToThoseWhoManageTheProviderOnly() throws Exception {
        Map<String, Object> managed = providerWithContacts("openaire", OWNER);
        Map<String, Object> other = providerWithContacts("other", "other@example.org");
        stub(ResourceKind.PROVIDER, managed);
        stub(ResourceKind.PROVIDER, other);
        when(bundles.search(eq(ResourceKind.PROVIDER), any()))
                .thenAnswer(call -> new Paging<>(2, 0, 2, List.of(managed, other), List.of()));
        when(groups.group(eq(ResourceKind.PROVIDER), any(), eq("structureTypes")))
                .thenReturn(Map.of("Research", List.of(managed, other)));
        when(groups.snippets(ResourceKind.PROVIDER)).thenReturn(payload -> payload);

        // null is the anonymous caller
        for (String email : new String[]{null, STRANGER, OWNER, OA_ADMIN, ADMIN}) {
            boolean managesFirst = email != null && !email.equals(STRANGER);
            boolean managesOther = ADMIN.equals(email) || OA_ADMIN.equals(email);
            expectContacts(get("/oa/providers/openaire"), email, "$", managesFirst);
            expectContacts(get("/oa/providers/other"), email, "$", managesOther);
            for (String view : List.of("full", "summary")) {
                expectContacts(get("/oa/providers").param("view", view), email, "$.results[0]", managesFirst);
                expectContacts(get("/oa/providers").param("view", view), email, "$.results[1]", managesOther);
                expectContacts(get("/oa/providers/by/structureTypes").param("view", view), email, "$.Research[0]",
                        managesFirst);
                expectContacts(get("/oa/providers/by/structureTypes").param("view", view), email, "$.Research[1]",
                        managesOther);
            }
        }
    }

    private void expectContacts(MockHttpServletRequestBuilder request, String email, String path,
                                boolean mainContact) throws Exception {
        mvc.perform(email == null ? request : as(request, email))
                .andExpect(status().isOk())
                .andExpect(jsonPath(path + ".publicContacts[0].email").value("public@example.org"))
                .andExpect(mainContact
                        ? jsonPath(path + ".mainContact.email").value("main@example.org")
                        : jsonPath(path + ".mainContact").doesNotExist());
    }

    private static Map<String, Object> providerWithContacts(String id, String user) {
        Map<String, Object> provider = bundle(ResourceKind.PROVIDER, id, "approved", true, Map.of(
                "name", id,
                "users", List.of(Map.of("email", user)),
                "mainContact", Map.of("firstName", "A", "email", "main@example.org"),
                "publicContacts", List.of(Map.of("email", "public@example.org"))));
        provider.put("properties", Map.of("channels", List.of("oa")));
        return provider;
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

    @Test
    void aChannelAdministratorListsTheBundlesOfTheirChannelsWhateverTheirActivity() throws Exception {
        when(bundles.search(eq(ResourceKind.SERVICE), any())).thenReturn(new Paging<>(0, 0, 0, List.of(), List.of()));

        mvc.perform(as(get("/node/services/bundles").param("channels", "node"), OA_ADMIN)).andExpect(status().isOk());
        mvc.perform(as(get("/node/services/bundles"), ADMIN)).andExpect(status().isOk());

        ArgumentCaptor<FacetFilter> filter = ArgumentCaptor.forClass(FacetFilter.class);
        verify(bundles, times(2)).search(eq(ResourceKind.SERVICE), filter.capture());
        assertEquals(List.of("oa"), filter.getAllValues().get(0).getFilter().get("channels"));
        assertNull(filter.getAllValues().get(0).getFilter().get("active"));
        assertNull(filter.getAllValues().get(1).getFilter().get("channels"));
    }

    @Test
    void theUsersOfAProviderListTheBundlesOfTheServicesItOwnsOrContributesTo() throws Exception {
        when(bundles.search(eq(ResourceKind.SERVICE), any())).thenReturn(new Paging<>(0, 0, 0, List.of(), List.of()));

        mvc.perform(as(get("/node/services/bundles/byOrganisation/openaire")
                .param("resourceOrganisation", "nodeprov").param("status", "pending"), OWNER))
                .andExpect(status().isOk());
        mvc.perform(as(get("/oa/services/bundles/byProvider/openaire").param("sort", "modifiedAt")
                .param("order", "desc"), OWNER)).andExpect(status().isOk());

        ArgumentCaptor<FacetFilter> filter = ArgumentCaptor.forClass(FacetFilter.class);
        verify(bundles, times(2)).search(eq(ResourceKind.SERVICE), filter.capture());
        FacetFilter owned = filter.getAllValues().get(0);
        assertEquals("openaire", owned.getFilter().get("resourceOrganisation"));
        assertFalse(owned.getSelectionFields().contains("resourceOrganisation"));
        assertEquals(List.of("pending"), owned.getFilter().get("status"));
        assertNull(owned.getFilter().get("active"));
        assertNull(owned.getFilter().get("channels"));
        assertEquals(Map.of("name", Map.of("order", "asc")), owned.getOrderBy());
        FacetFilter contributed = filter.getAllValues().get(1);
        assertEquals("openaire", contributed.getFilter().get("resourceProviders"));
        assertFalse(contributed.getSelectionFields().contains("resourceProviders"));
        assertEquals(Map.of("modifiedAt", Map.of("order", "desc")), contributed.getOrderBy());
    }

    @Test
    void theServicesOfAProviderAreForItsUsersAndGlobalReviewers() throws Exception {
        when(bundles.search(eq(ResourceKind.SERVICE), any())).thenReturn(new Paging<>(0, 0, 0, List.of(), List.of()));

        List<String> paths = List.of("/oa/services/bundles/byOrganisation/", "/oa/services/bundles/byProvider/");
        for (String path : paths) {
            mvc.perform(get(path + "openaire")).andExpect(status().isUnauthorized());
        }
        for (String path : paths) {
            mvc.perform(as(get(path + "openaire"), STRANGER)).andExpect(status().isForbidden());
            mvc.perform(as(get(path + "openaire"), OA_ADMIN)).andExpect(status().isForbidden());
            mvc.perform(as(get(path + "nodeprov"), OWNER)).andExpect(status().isForbidden());
            mvc.perform(as(get(path + "missing"), OWNER)).andExpect(status().isForbidden());
            mvc.perform(as(get(path + "openaire"), ADMIN)).andExpect(status().isOk());
        }
        verify(bundles, times(2)).search(eq(ResourceKind.SERVICE), any());
    }

    @Test
    void theUsersOfAResourceProviderViewTheBundleOfTheServiceWithoutManagingIt() throws Exception {
        Map<String, Object> partner = bundle(ResourceKind.SERVICE, "nodeprov.tool", "pending", false,
                Map.of("name", "Tool", "resourceOrganisation", "nodeprov", "resourceProviders", List.of("openaire")));
        partner.put("properties", Map.of("channels", List.of("node")));
        stub(ResourceKind.SERVICE, partner);

        mvc.perform(as(get("/node/services/nodeprov.tool"), OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceProviders[0]").value("openaire"));
        mvc.perform(as(get("/oa/services/nodeprov.tool"), OWNER)).andExpect(status().isOk());
        mvc.perform(as(get("/node/services/nodeprov.tool"), STRANGER)).andExpect(status().isNotFound());
        mvc.perform(as(get("/node/services/bundles/missing"), OWNER)).andExpect(status().isForbidden());

        mvc.perform(as(get("/node/services/bundles/nodeprov.tool"), OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"));
        mvc.perform(as(get("/node/services/bundles/nodeprov.tool"), STRANGER)).andExpect(status().isForbidden());
        mvc.perform(as(put("/node/services/nodeprov.tool").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Tool\"}"), OWNER)).andExpect(status().isForbidden());
        mvc.perform(as(delete("/node/services/nodeprov.tool"), OWNER)).andExpect(status().isForbidden());
        mvc.perform(as(post("/node/services/bundles/nodeprov.tool/approve"), OWNER)).andExpect(status().isForbidden());
        verify(bundles, never()).update(any(), any(), any(), any(), anyBoolean(), any());
        verify(bundles, never()).delete(any(), any(), any());
    }

    @Test
    void aChannelAdministratorReviewsRecordsListedOnTheirChannel() throws Exception {
        when(bundles.approve(eq(ResourceKind.SERVICE), any(), any(), any()))
                .thenReturn(service("openaire.openplato", "approved", true, List.of("oa")));

        mvc.perform(as(post("/oa/services/bundles/openaire.openplato/approve"), OA_ADMIN)).andExpect(status().isOk());
        mvc.perform(as(post("/node/services/bundles/openaire.shared/approve"), OA_ADMIN)).andExpect(status().isOk());
        mvc.perform(as(post("/oa/services/bundles/openaire.graph/approve"), OA_ADMIN))
                .andExpect(status().isForbidden());
        mvc.perform(as(get("/oa/services/bundles/openaire.openplato"), OA_ADMIN)).andExpect(status().isOk());
        mvc.perform(as(get("/node/services/bundles/openaire.graph"), OA_ADMIN)).andExpect(status().isForbidden());
    }

    @Test
    void aChannelAdministratorChangesOnlyTheirOwnChannelAndKeepsOneListed() throws Exception {
        when(bundles.setChannel(any(), any(), any(), anyBoolean(), any())).thenReturn(List.of("node"));

        mvc.perform(as(delete("/oa/services/bundles/openaire.shared/channels/oa"), OA_ADMIN))
                .andExpect(status().isOk());
        mvc.perform(as(delete("/oa/services/bundles/openaire.argos/channels/oa"), OA_ADMIN))
                .andExpect(status().isForbidden());
        mvc.perform(as(put("/oa/services/bundles/openaire.argos/channels/node"), OA_ADMIN))
                .andExpect(status().isForbidden());
        mvc.perform(as(put("/node/services/bundles/openaire.graph/channels/oa"), OA_ADMIN))
                .andExpect(status().isForbidden());
        mvc.perform(as(delete("/oa/services/bundles/openaire.argos/channels/oa"), ADMIN)).andExpect(status().isOk());
    }

    @Test
    void aChannelAdministratorAddsAndMovesServicesUnderProvidersOfTheirChannelOnly() throws Exception {
        when(bundles.create(eq(ResourceKind.SERVICE), any(), any(), any()))
                .thenReturn(service("openaire.new", "pending", false, List.of("oa")));
        when(bundles.update(eq(ResourceKind.SERVICE), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(service("openaire.argos", "approved", true, List.of("oa")));

        mvc.perform(as(post("/oa/services"), OA_ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"resourceOrganisation\":\"openaire\"}"))
                .andExpect(status().isCreated());
        mvc.perform(as(post("/oa/services"), OA_ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"resourceOrganisation\":\"nodeprov\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(as(put("/oa/services/openaire.argos"), OA_ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Argos\",\"resourceOrganisation\":\"nodeprov\"}"))
                .andExpect(status().isOk());
        verify(bundles).update(eq(ResourceKind.SERVICE), any(), any(), any(), eq(false), any());
    }

    @Test
    void aChannelAdministratorRegistersProvidersWithoutAnInvitation() throws Exception {
        when(bundles.create(eq(ResourceKind.PROVIDER), any(), any(), any()))
                .thenReturn(bundle(ResourceKind.PROVIDER, "new", "pending", false, Map.of("name", "New")));

        mvc.perform(as(post("/oa/providers"), OA_ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"abbreviation\":\"new\"}"))
                .andExpect(status().isCreated());
        verify(invitations, never()).verify(any(), any());
    }

    private void stubSummaries(ResourceKind kind) {
        when(groups.snippets(kind)).thenReturn(payload -> Map.of("id", payload.get("id"), "name", payload.get("name")));
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
