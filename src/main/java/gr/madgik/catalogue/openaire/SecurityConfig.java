package gr.madgik.catalogue.openaire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    public SecurityConfig() {

    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers()
                .xssProtection();

        http
                .authorizeRequests(authorizeRequests -> authorizeRequests
                        .anyRequest().permitAll())
                .cors()
                .and()
                .csrf().disable();
        return http.build();
    }
}
