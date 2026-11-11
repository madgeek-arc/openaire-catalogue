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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2TokenIntrospectionClaimNames;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Browsers sign in through OIDC ({@code /oauth2/authorization/{registration}}) and keep a session; API clients send an
 * opaque bearer token, when an introspection endpoint is configured. Authorities come from {@link Roles}.
 * Every route needs an administrator unless it is listed here, so routes added by libraries are closed by default.
 * The public routes are open at the URL level only. Unauthenticated calls get 401 rather than a login redirect.
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
                        .anyRequest().hasAuthority(Roles.ROLE_ADMIN))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);

        ClientRegistrationRepository repository = registrations.getIfAvailable();
        if (repository != null) {
            http
                    .oauth2Login(login -> login
                            .successHandler((request, response, auth) ->
                                    response.sendRedirect(properties.getLoginRedirect())))
                    .logout(logout -> logout
                            .logoutSuccessHandler(logoutSuccessHandler(repository))
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
     * Replaces the authorities of an OIDC or OAuth2 login with those {@link Roles} grants to the user's email.
     */
    @Bean
    GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new HashSet<>();
            for (GrantedAuthority authority : authorities) {
                String email = null;
                if (authority instanceof OidcUserAuthority oidc) {
                    email = oidc.getUserInfo() != null ? oidc.getUserInfo().getEmail()
                            : (String) oidc.getAttributes().get("email");
                } else if (authority instanceof OAuth2UserAuthority oauth2) {
                    email = (String) oauth2.getAttributes().get("email");
                }
                if (email != null) {
                    mapped.addAll(roles.authorities(email));
                }
            }
            return mapped;
        };
    }

    /**
     * Grants the authorities of a bearer token from the email in its introspection result. A token without an email is
     * refused.
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.opaquetoken", name = "introspection-uri")
    OpaqueTokenAuthenticationConverter opaqueTokenAuthenticationConverter() {
        return (token, principal) -> {
            Map<String, Object> attributes = principal.getAttributes();
            Object email = attributes.get("email");
            if (email == null || email.toString().isBlank()) {
                throw new InvalidBearerTokenException("The token has no email");
            }
            Set<GrantedAuthority> authorities = roles.authorities(email.toString());
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

    private LogoutSuccessHandler logoutSuccessHandler(ClientRegistrationRepository repository) {
        OidcClientInitiatedLogoutSuccessHandler handler = new OidcClientInitiatedLogoutSuccessHandler(repository);
        handler.setPostLogoutRedirectUri(properties.getLogoutRedirect());
        handler.setDefaultTargetUrl(properties.getLogoutRedirect());
        return handler;
    }
}
