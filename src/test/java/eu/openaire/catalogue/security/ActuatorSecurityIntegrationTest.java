package eu.openaire.catalogue.security;

import eu.openaire.catalogue.config.OpenAIREProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.logging.LogFileWebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.logging.LoggersEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.health.autoconfigure.actuate.endpoint.HealthEndpointAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.autoconfigure.actuate.web.server.TomcatServletManagementContextAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.actuate.endpoint.web.WebMvcHealthEndpointExtensionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts the application server and the management server on their own ports, with the application's properties,
 * security filter chain and an in-memory session store, and checks which actuator endpoints each port serves and to
 * whom. Every endpoint is exposed, so that the rules are checked for any endpoint a deployment may enable.
 */
@SpringBootTest(classes = ActuatorSecurityIntegrationTest.App.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0", "management.endpoints.web.exposure.include=*"})
class ActuatorSecurityIntegrationTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int serverPort;

    @Value("${local.management.port}")
    private int managementPort;

    @Autowired
    private MapSessionRepository sessions;

    @RestController
    static class Ping {

        @GetMapping("/oa/ping")
        String ping() {
            return "pong";
        }
    }

    @SpringBootConfiguration
    @ImportAutoConfiguration({
            TomcatServletWebServerAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            ObservationAutoConfiguration.class, WebMvcObservationAutoConfiguration.class,
            MetricsAutoConfiguration.class, PrometheusMetricsExportAutoConfiguration.class,
            EndpointAutoConfiguration.class, JacksonEndpointAutoConfiguration.class,
            WebEndpointAutoConfiguration.class, ManagementContextAutoConfiguration.class,
            ServletManagementContextAutoConfiguration.class, TomcatServletManagementContextAutoConfiguration.class,
            HealthContributorAutoConfiguration.class, HealthContributorRegistryAutoConfiguration.class,
            HealthEndpointAutoConfiguration.class, WebMvcHealthEndpointExtensionAutoConfiguration.class,
            LogFileWebEndpointAutoConfiguration.class, EnvironmentEndpointAutoConfiguration.class,
            LoggersEndpointAutoConfiguration.class, SessionAutoConfiguration.class,
            SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class,
            SecurityFilterAutoConfiguration.class})
    @EnableSpringHttpSession
    @Import(SecurityConfig.class)
    static class App {

        @Bean
        MapSessionRepository sessionRepository() {
            return new MapSessionRepository(new ConcurrentHashMap<>());
        }

        @Bean
        OpenAIREProperties properties() {
            return new OpenAIREProperties();
        }

        @Bean
        Roles roles(OpenAIREProperties properties) {
            return new Roles(properties);
        }

        @Bean
        Ping ping() {
            return new Ping();
        }
    }

    @Test
    void healthAndPrometheusAreOpenOnTheManagementPort() throws Exception {
        assertEquals(200, get(serverPort, "/api/oa/ping").statusCode());

        assertEquals(200, get(managementPort, "/actuator/health").statusCode());
        HttpResponse<String> scrape = get(managementPort, "/actuator/prometheus");
        assertEquals(200, scrape.statusCode());
        assertTrue(scrape.body().contains("http_server_requests_seconds_bucket{"), "request duration histogram");
        assertTrue(scrape.body().contains("uri=\"/oa/ping\""), "requests tagged with their route");
    }

    @Test
    void otherEndpointsOnTheManagementPortNeedASignedInAdministrator() throws Exception {
        String user = signIn(false);
        String admin = signIn(true);
        for (String path : new String[]{"/actuator", "/actuator/env", "/actuator/loggers"}) {
            assertEquals(401, get(managementPort, path).statusCode(), path);
            assertEquals(403, get(managementPort, path, user).statusCode(), path);
            assertEquals(200, get(managementPort, path, admin).statusCode(), path);
        }
    }

    @Test
    void theApplicationPortServesNoEndpoints() throws Exception {
        for (String path : new String[]{"/api/actuator/health", "/api/actuator/prometheus", "/api/actuator/logfile"}) {
            assertEquals(401, get(serverPort, path).statusCode(), path);
        }
    }

    private HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
        return get(port, path, null);
    }

    private HttpResponse<String> get(int port, String path, String session) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (session != null) {
            request.header("Cookie", "SESSION=" + session);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Stores a signed-in session and returns its cookie value.
     */
    private String signIn(boolean admin) {
        var authorities = admin ? AuthorityUtils.createAuthorityList(Roles.ROLE_ADMIN) : AuthorityUtils.NO_AUTHORITIES;
        MapSession session = sessions.createSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated("user@example.org", null, authorities)));
        sessions.save(session);
        return Base64.getEncoder().encodeToString(session.getId().getBytes(StandardCharsets.UTF_8));
    }
}
