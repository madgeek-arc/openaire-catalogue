package eu.openaire.catalogue.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedirectTargetsTest {

    private final RedirectTargets targets =
            new RedirectTargets(List.of("https://OA.example/", "https://node.example:8443", "not an origin"));

    @Test
    void acceptsUrlsOnAnAllowedOrigin() {
        assertEquals(Optional.of(URI.create("https://oa.example/catalogue?tab=1")),
                targets.allowed("https://oa.example/catalogue?tab=1"));
        assertEquals(Optional.of(URI.create("https://oa.example:443/")), targets.allowed("https://oa.example:443/"));
        assertTrue(targets.allowed("https://node.example:8443/x").isPresent());
    }

    @Test
    void refusesEverythingElse() {
        for (String value : List.of("https://node.example/x", "http://oa.example", "https://oa.example.evil.org",
                "https://oa.example@evil.org", "https://evil.org@oa.example", "//oa.example/x", "/relative",
                "javascript:alert(1)", "https:\\\\evil.org", "https://evil.org\\@oa.example", "")) {
            assertTrue(targets.allowed(value).isEmpty(), value);
        }
    }

    @Test
    void theParameterComesFirstThenTheOriginOfTheReferer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "https://node.example:8443/services/1");
        assertEquals(Optional.of(URI.create("https://node.example:8443")), targets.resolve(request));

        request.setParameter(RedirectTargets.PARAMETER, "https://oa.example/home");
        assertEquals(Optional.of(URI.create("https://oa.example/home")), targets.resolve(request));

        request.setParameter(RedirectTargets.PARAMETER, "https://evil.org/");
        assertEquals(Optional.of(URI.create("https://node.example:8443")), targets.resolve(request));
    }

    @Test
    void nothingAllowedMeansNoTarget() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertTrue(targets.resolve(request).isEmpty());
        request.addHeader("Referer", "https://evil.org/");
        assertTrue(targets.resolve(request).isEmpty());
    }
}
