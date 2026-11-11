package eu.openaire.catalogue.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.net.URI;

/**
 * Returns the browser, after a login, to the target chosen by {@link RedirectTargets} when the login started. The
 * target travels in the attributes of the authorization request, so concurrent logins in one session each keep their
 * own.
 */
public final class LoginRedirects {

    private static final String ATTRIBUTE = LoginRedirects.class.getName() + ".target";

    private LoginRedirects() {
    }

    /**
     * Adds the target of the request to the authorization request.
     */
    public static OAuth2AuthorizationRequestResolver resolver(ClientRegistrationRepository registrations,
                                                              RedirectTargets targets) {
        DefaultOAuth2AuthorizationRequestResolver delegate = new DefaultOAuth2AuthorizationRequestResolver(
                registrations, OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return withTarget(delegate.resolve(request), request);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                return withTarget(delegate.resolve(request, clientRegistrationId), request);
            }

            private OAuth2AuthorizationRequest withTarget(OAuth2AuthorizationRequest authorization,
                                                          HttpServletRequest request) {
                if (authorization == null) {
                    return null;
                }
                return targets.resolve(request)
                        .map(target -> OAuth2AuthorizationRequest.from(authorization)
                                .attributes(attributes -> attributes.put(ATTRIBUTE, target.toString()))
                                .build())
                        .orElse(authorization);
            }
        };
    }

    /**
     * Stores authorization requests in the session, and exposes the target of the one completed to the success handler
     * of the same request.
     */
    public static AuthorizationRequestRepository<OAuth2AuthorizationRequest> repository() {
        HttpSessionOAuth2AuthorizationRequestRepository delegate = new HttpSessionOAuth2AuthorizationRequestRepository();
        return new AuthorizationRequestRepository<>() {
            @Override
            public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
                return delegate.loadAuthorizationRequest(request);
            }

            @Override
            public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorization, HttpServletRequest request,
                                                 HttpServletResponse response) {
                delegate.saveAuthorizationRequest(authorization, request, response);
            }

            @Override
            public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                         HttpServletResponse response) {
                OAuth2AuthorizationRequest authorization = delegate.removeAuthorizationRequest(request, response);
                if (authorization != null && authorization.getAttribute(ATTRIBUTE) != null) {
                    request.setAttribute(ATTRIBUTE, authorization.getAttribute(ATTRIBUTE));
                }
                return authorization;
            }
        };
    }

    /**
     * Redirects to the target of the completed login, or else to the given default.
     */
    public static AuthenticationSuccessHandler successHandler(String defaultTarget) {
        return (request, response, authentication) -> {
            Object target = request.getAttribute(ATTRIBUTE);
            response.sendRedirect(target != null ? URI.create(target.toString()).toASCIIString() : defaultTarget);
        };
    }
}
