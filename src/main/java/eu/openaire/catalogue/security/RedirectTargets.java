package eu.openaire.catalogue.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Where the browser returns after a login or logout: the {@value #PARAMETER} query parameter, or else the origin of the
 * {@code Referer} header, if its origin is one of the allowed origins.
 */
public class RedirectTargets {

    public static final String PARAMETER = "redirectUrl";

    private static final Logger logger = LoggerFactory.getLogger(RedirectTargets.class);

    private final Set<String> allowedOrigins = new LinkedHashSet<>();

    public RedirectTargets(Collection<String> allowedOrigins) {
        for (String origin : allowedOrigins) {
            parse(origin).map(RedirectTargets::origin).ifPresentOrElse(this.allowedOrigins::add,
                    () -> logger.warn("Ignoring allowed origin '{}', which is not an http(s) origin", origin));
        }
    }

    /**
     * @return the target the request asks for, as an absolute URI, or empty if it names none that is allowed
     */
    public Optional<URI> resolve(HttpServletRequest request) {
        String requested = request.getParameter(PARAMETER);
        if (requested != null && !requested.isBlank()) {
            Optional<URI> target = allowed(requested);
            if (target.isPresent()) {
                return target;
            }
            logger.warn("Ignoring {} '{}', whose origin is not allowed", PARAMETER, requested);
        }
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer != null && !referer.isBlank()) {
            return allowed(referer).map(uri -> URI.create(origin(uri)));
        }
        return Optional.empty();
    }

    /**
     * @return the value as an absolute URI, if its origin is allowed
     */
    public Optional<URI> allowed(String value) {
        return parse(value).filter(uri -> allowedOrigins.contains(origin(uri)));
    }

    /**
     * The scheme, host and port of the URI, lowercase and without a default port.
     */
    public static String origin(URI uri) {
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        int port = uri.getPort();
        boolean defaultPort = port == -1 || ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port);
    }

    /**
     * @return the value as an absolute http(s) URI with a host and no user info
     */
    private static Optional<URI> parse(String value) {
        if (value == null || value.isBlank() || value.contains("\\")) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null || uri.getRawUserInfo() != null) {
                return Optional.empty();
            }
            return Optional.of(URI.create(uri.toASCIIString()));
        } catch (URISyntaxException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
