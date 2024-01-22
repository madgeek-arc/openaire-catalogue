package gr.madgik.catalogue.openaire.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final AuthenticationSuccessHandler authSuccessHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ApplicationProperties applicationProperties;

    public SecurityConfig(AuthenticationSuccessHandler authSuccessHandler,
                          ClientRegistrationRepository clientRegistrationRepository,
                          ApplicationProperties applicationProperties) {
        this.authSuccessHandler = authSuccessHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.applicationProperties = applicationProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers()
                .xssProtection();

        http
                .authorizeRequests(authorizeRequests -> authorizeRequests
                        .antMatchers(HttpMethod.GET, "/forms/**").permitAll()
                        .antMatchers(HttpMethod.GET, "/vocabularies/**").permitAll()
                        .regexMatchers("/dump/.*", "/restore/", "/resources.*", "/version.*", "/items.*", "/resourceType.*", "/search.*", "/logs.*", "/forms.*", "/vocabularies.*").hasAnyAuthority("ADMIN")
                        .anyRequest().permitAll())

                .oauth2Login(oauth2login -> oauth2login
                        .successHandler(authSuccessHandler))

                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .deleteCookies()
                        .clearAuthentication(true)
                        .invalidateHttpSession(true))

                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(
                        this.clientRegistrationRepository);

        oidcLogoutSuccessHandler.setPostLogoutRedirectUri(applicationProperties.getLogoutRedirect());

        return oidcLogoutSuccessHandler;
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                if (authority instanceof OidcUserAuthority) {
                    OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;

                    Map<String, Object> userAttributes = oidcUserAuthority.getAttributes();
                    addAuthorities(userAttributes, mappedAuthorities);

                } else if (authority instanceof OAuth2UserAuthority) {
                    OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;

                    Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();
                    addAuthorities(userAttributes, mappedAuthorities);

                }
            });

            return mappedAuthorities;
        };
    }

    /**
     * Adds one or more GrantedAuthority's to the user's mappedAuthorities
     *
     * @param userAttributes
     * @param grantedAuthorities
     */
    private void addAuthorities(Map<String, Object> userAttributes, Set<GrantedAuthority> grantedAuthorities) {
        if (userAttributes != null && applicationProperties.getAdmins().contains(userAttributes.get("email"))) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ADMIN"));
        }

        if (userAttributes != null && applicationProperties.getOnboardingTeam().contains(userAttributes.get("email"))) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ONBOARDING_TEAM"));
        }
    }
}
