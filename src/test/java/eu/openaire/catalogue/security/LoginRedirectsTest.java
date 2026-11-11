package eu.openaire.catalogue.security;

import eu.openaire.catalogue.config.OpenAIREProperties;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real security filter chain with an OIDC registration, to check where login and logout return the browser.
 */
class LoginRedirectsTest {

    private static final String OA = "https://oa.example";
    private static final String NODE = "https://node.example:8443";

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class App {

        @Bean
        OpenAIREProperties properties() {
            OpenAIREProperties properties = new OpenAIREProperties();
            properties.setAllowedOrigins(List.of(OA, NODE));
            properties.setLoginRedirect("https://default.example/in");
            properties.setLogoutRedirect("https://default.example");
            return properties;
        }

        @Bean
        Roles roles(OpenAIREProperties properties) {
            return new Roles(properties);
        }

        @Bean
        ClientRegistrationRepository registrations() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("openaire")
                    .clientId("catalogue")
                    .clientSecret("secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "email")
                    .authorizationUri("https://idp.example/auth")
                    .tokenUri("https://idp.example/token")
                    .jwkSetUri("https://idp.example/certs")
                    .providerConfigurationMetadata(Map.of("end_session_endpoint", "https://idp.example/logout"))
                    .build());
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
    void eachLoginReturnsToTheFrontendThatStartedIt() throws Exception {
        MockHttpSession session = new MockHttpSession();
        assertEquals(OA + "/services?tab=1", completeLogin(session, startLogin(session, OA + "/services?tab=1", null)));
        assertEquals(NODE, completeLogin(session, startLogin(session, null, NODE + "/providers/x")));
        assertEquals("https://default.example/in", completeLogin(session, startLogin(session, "https://evil.org/", null)));
        assertEquals("https://default.example/in", completeLogin(session, startLogin(session, null, null)));
    }

    @Test
    void logoutReturnsToTheOriginOfTheFrontend() throws Exception {
        Map<String, String> query = logout("redirectUrl", OA + "/a?b=1&c=%20%7Bx%7D");
        assertEquals("https://idp.example/logout", query.get("endpoint"));
        assertEquals(OA, query.get("post_logout_redirect_uri"));
        assertEquals("id-token", query.get("id_token_hint"));

        assertEquals("https://default.example", logout("redirectUrl", "https://evil.org/").get("post_logout_redirect_uri"));
    }

    private String startLogin(MockHttpSession session, String redirectUrl, String referer) throws Exception {
        var request = get("/oauth2/authorization/openaire").session(session);
        if (redirectUrl != null) {
            request.param("redirectUrl", redirectUrl);
        }
        if (referer != null) {
            request.header("Referer", referer);
        }
        String location = mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn().getResponse()
                .getRedirectedUrl();
        return decode(UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("state"));
    }

    /**
     * Does what the login filter does once the provider calls back: removes the authorization request, then runs the
     * success handler.
     */
    private String completeLogin(MockHttpSession session, String state) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/openaire");
        request.setSession(session);
        request.setParameter("state", state);
        MockHttpServletResponse response = new MockHttpServletResponse();
        LoginRedirects.repository().removeAuthorizationRequest(request, response);
        LoginRedirects.successHandler(context.getBean(OpenAIREProperties.class).getLoginRedirect())
                .onAuthenticationSuccess(request, response, null);
        return response.getRedirectedUrl();
    }

    private Map<String, String> logout(String parameter, String value) throws Exception {
        DefaultOidcUser user = new DefaultOidcUser(AuthorityUtils.createAuthorityList("ROLE_USER"),
                new OidcIdToken("id-token", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "user")));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(new OAuth2AuthenticationToken(user, user.getAuthorities(), "openaire")));
        String location = mvc.perform(get("/logout").session(session).param(parameter, value))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
        var uri = UriComponentsBuilder.fromUriString(location).build();
        return Map.of("endpoint", uri.getScheme() + "://" + uri.getHost() + uri.getPath(),
                "post_logout_redirect_uri", decode(uri.getQueryParams().getFirst("post_logout_redirect_uri")),
                "id_token_hint", uri.getQueryParams().getFirst("id_token_hint"));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
