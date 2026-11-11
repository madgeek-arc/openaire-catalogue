package eu.openaire.catalogue.security;

import eu.openaire.catalogue.config.CatalogueProperties;
import eu.openaire.catalogue.config.OpenAIREProperties;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real security filter chain in front of a controller that answers every path, so that each status comes
 * from the URL rules alone.
 */
class SecurityConfigTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @RestController
    static class AnyRoute {

        @RequestMapping("/**")
        String ok() {
            return "ok";
        }
    }

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class App {

        @Bean
        OpenAIREProperties properties() {
            return new OpenAIREProperties();
        }

        @Bean
        CatalogueProperties catalogueProperties() {
            return new CatalogueProperties();
        }

        @Bean
        Roles roles(CatalogueProperties properties) {
            return new Roles(properties);
        }

        @Bean
        AnyRoute anyRoute() {
            return new AnyRoute();
        }
    }

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(App.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void publicRoutesAreOpenToEveryone() throws Exception {
        expect(status().isOk(), get("/oa/services"), get("/oa/providers/openaire"), post("/oa/services"),
                get("/node/services"), get("/node/datasources/x"), post("/node/providers"),
                get("/eosc/services"), get("/vocabularies/byType"), get("/vocabulary/types/Provider"),
                get("/dsm/datasources"), get("/forms/models"), get("/models/m-oa-service"),
                get("/v3/api-docs"), get("/actuator/health"));
    }

    @Test
    void unlistedRoutesNeedASignedInAdministrator() throws Exception {
        MockHttpServletRequestBuilder[] adminOnly = {get("/items"), post("/items"), get("/version"),
                get("/version/service"), get("/versions"), get("/records/service"), post("/resourceTypes"),
                post("/forms/models"), delete("/models"), get("/resourcesync/x"), get("/schemaService/x"),
                get("/dump"), get("/anything/new")};
        expect(status().isUnauthorized(), adminOnly);
        expect(status().isForbidden(), signedIn(false, adminOnly));
        expect(status().isOk(), signedIn(true, adminOnly));
    }

    @Test
    void userRoutesNeedASignedInUser() throws Exception {
        MockHttpServletRequestBuilder[] userOnly = {get("/user/info"), post("/invitations"), post("/request/default")};
        expect(status().isUnauthorized(), userOnly);
        expect(status().isOk(), signedIn(false, userOnly));
    }

    private void expect(ResultMatcher matcher, MockHttpServletRequestBuilder... requests) throws Exception {
        for (MockHttpServletRequestBuilder request : requests) {
            mvc.perform(request).andExpect(matcher);
        }
    }

    private static MockHttpServletRequestBuilder[] signedIn(boolean admin, MockHttpServletRequestBuilder... requests) {
        var authorities = admin ? AuthorityUtils.createAuthorityList(Roles.ROLE_ADMIN) : AuthorityUtils.NO_AUTHORITIES;
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated("user@example.org", null, authorities)));
        for (MockHttpServletRequestBuilder request : requests) {
            request.session(session);
        }
        return requests;
    }
}
