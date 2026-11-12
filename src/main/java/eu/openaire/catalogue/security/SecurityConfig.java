package eu.openaire.catalogue.security;

import eu.openaire.catalogue.config.OpenAIREProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.DefaultLoginPageConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Browsers sign in through OIDC ({@code /oauth2/authorization/{registration}}) and keep a session; API clients send an
 * opaque bearer token, when an introspection endpoint is configured. Authorities come from {@link Roles}. Login and
 * logout return the browser to the frontend that started them, as chosen by {@link RedirectTargets}.
 * Every route needs an administrator unless it is listed here, so routes added by libraries are closed by default.
 * The public routes are open at the URL level only. Unauthenticated calls get 401 rather than a login redirect.
 * With {@code openaire.login-page}, {@code /login} lists the configured providers; the 401 is registered as a default
 * entry point because an explicit one disables that page.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Open to everyone at the URL level.
     */
    static final String[] PUBLIC_ROUTES = {
            "/oa/**", "/node/**", "/eosc/**", "/vocabulary/**", "/vocabularies/**", "/dsm/**", "/provide/**",
            "/oauth2/**", "/login/**", "/logout", "/error",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/actuator/health/**"};

    /**
     * Open to signed-in users; {@code /request/**} is an outbound HTTP proxy.
     */
    static final String[] AUTHENTICATED_ROUTES = {"/user/**", "/invitations/**", "/request/**"};

    /**
     * Readable by everyone, because forms are rendered from the models.
     */
    static final String[] PUBLIC_READ_ROUTES = {"/forms/**", "/models/**"};

    private final OpenAIREProperties properties;
    private final Roles roles;

    public SecurityConfig(OpenAIREProperties properties, Roles roles) {
        this.properties = properties;
        this.roles = roles;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, ObjectProvider<ClientRegistrationRepository> registrations,
                                    ObjectProvider<OpaqueTokenAuthenticationConverter> tokenConverter) {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_READ_ROUTES).permitAll()
                        .requestMatchers(AUTHENTICATED_ROUTES).authenticated()
                        .anyRequest().hasRole("ADMIN"))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                AnyRequestMatcher.INSTANCE))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);
        if (!properties.isLoginPage()) {
            http.removeConfigurer(DefaultLoginPageConfigurer.class);
        }

        ClientRegistrationRepository repository = registrations.getIfAvailable();
        if (repository != null) {
            RedirectTargets targets = new RedirectTargets(properties.getAllowedOrigins());
            http
                    .oauth2Login(login -> login
                            .authorizationEndpoint(endpoint -> endpoint
                                    .authorizationRequestResolver(LoginRedirects.resolver(repository, targets))
                                    .authorizationRequestRepository(LoginRedirects.repository()))
                            .userInfoEndpoint(userInfo -> userInfo
                                    .oidcUserService(new RoleOidcUserService(roles,
                                            !properties.getRoles().getIdpRoleClaims().isEmpty()))
                                    .userService(oauth2UserService()))
                            .successHandler(LoginRedirects.successHandler(properties.getLoginRedirect())))
                    .logout(logout -> logout
                            .logoutSuccessHandler(logoutSuccessHandler(repository, targets))
                            .clearAuthentication(true)
                            .invalidateHttpSession(true));
        }
        OpaqueTokenAuthenticationConverter converter = tokenConverter.getIfAvailable();
        if (converter != null) {
            http.oauth2ResourceServer(server -> server.opaqueToken(token -> token.authenticationConverter(converter)));
        }
        return http.build();
    }

    /**
     * Loads users of plain OAuth2 providers with the authorities {@link Roles} grants to their email.
     */
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User user = delegate.loadUser(request);
            Object email = user.getAttributes().get("email");
            String nameAttribute = request.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
                    .getUserNameAttributeName();
            return new DefaultOAuth2User(roles.authorities(email != null ? email.toString() : null),
                    user.getAttributes(), nameAttribute);
        };
    }

    /**
     * Grants the authorities of a bearer token from the email and identity provider roles in its introspection result.
     * A token without an email is refused. A {@code {client}} segment of a role claim path stands for the
     * introspection client id.
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.opaquetoken", name = "introspection-uri")
    OpaqueTokenAuthenticationConverter opaqueTokenAuthenticationConverter(
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id:}") String clientId) {
        return (token, principal) -> {
            Map<String, Object> attributes = principal.getAttributes();
            Object email = attributes.get("email");
            if (email == null || email.toString().isBlank()) {
                throw new InvalidBearerTokenException("The token has no email");
            }
            Set<GrantedAuthority> authorities = roles.authorities(email.toString(),
                    roles.idpRoles(attributes, clientId));
            DefaultOAuth2AuthenticatedPrincipal enriched =
                    new DefaultOAuth2AuthenticatedPrincipal(principal.getName(), attributes, authorities);
            Instant issuedAt = enriched.getAttribute(OAuth2TokenIntrospectionClaimNames.IAT);
            Instant expiresAt = enriched.getAttribute(OAuth2TokenIntrospectionClaimNames.EXP);
            OAuth2AccessToken accessToken =
                    new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token, issuedAt, expiresAt);
            return new BearerTokenAuthentication(enriched, accessToken, authorities);
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (!properties.getAllowedOrigins().isEmpty()) {
            CorsConfiguration cors = new CorsConfiguration();
            cors.setAllowedOrigins(properties.getAllowedOrigins());
            cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            cors.setAllowedHeaders(List.of("*"));
            cors.setAllowCredentials(true);
            source.registerCorsConfiguration("/**", cors);
        }
        return source;
    }

    /**
     * Ends the session at the provider, which then returns the browser to the frontend that asked for the logout, or
     * else to {@code openaire.logout-redirect}. The provider must accept every URL on an allowed origin as a
     * post-logout redirect URI, as Keycloak does for {@code <origin>/*}.
     */
    private LogoutSuccessHandler logoutSuccessHandler(ClientRegistrationRepository repository,
                                                      RedirectTargets targets) {
        return (request, response, authentication) -> {
            String target = targets.resolve(request)
                    .map(URI::toASCIIString)
                    .orElse(properties.getLogoutRedirect());
            OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(repository);
            handler.setPostLogoutRedirectUri(target);
            handler.setDefaultTargetUrl(target);
            handler.onLogoutSuccess(request, response, authentication);
        };
    }
}
