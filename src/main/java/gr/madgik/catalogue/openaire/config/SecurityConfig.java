package gr.madgik.catalogue.openaire.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final AuthenticationSuccessHandler authSuccessHandler;
    private final ApplicationProperties applicationProperties;

    public SecurityConfig(AuthenticationSuccessHandler authSuccessHandler,
                          ApplicationProperties applicationProperties) {
        this.authSuccessHandler = authSuccessHandler;
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
                .oauth2Login()
                .successHandler(authSuccessHandler)
                .and()
                .logout().logoutSuccessUrl(applicationProperties.getLogoutRedirect())
                .deleteCookies().clearAuthentication(true).invalidateHttpSession(true)
                .and()
                .cors()
                .and()
                .csrf().disable();
        return http.build();
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                String userEmail = null;

                if (authority instanceof OidcUserAuthority oidcUserAuthority) {

                    OidcIdToken idToken = oidcUserAuthority.getIdToken();
                    OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();

                    if (idToken != null) {
                        userEmail = idToken.getClaims().get("email").toString();
                    } else if (userInfo != null) {
                        userEmail = userInfo.getEmail();
                    } else if (((OidcUserAuthority) authority).getAttributes() != null
                                && ((OidcUserAuthority) authority).getAttributes().containsKey("email")) {
                        userEmail = ((OidcUserAuthority) authority).getAttributes().get("email").toString();
                    }

                } else if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
                    Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();
                    userEmail = userAttributes.get("email").toString();
                }

                // Map claims found to one or more GrantedAuthority's and add it to mappedAuthorities
                if (applicationProperties.getAdmins().contains(userEmail)) {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ADMIN"));
                }

                if (applicationProperties.getOnboardingTeam().contains(userEmail)) {
                    mappedAuthorities.add(new SimpleGrantedAuthority("ONBOARDING_TEAM"));
                }
            });

            return mappedAuthorities;
        };
    }
}
