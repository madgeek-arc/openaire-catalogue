package eu.openaire.catalogue.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the OIDC user and replaces their authorities with those {@link Roles} grants to their email and identity
 * provider roles. Roles are read from the ID token and user info claims, and from the access token when it is a JWT
 * signed by the provider, because some providers put roles in the access token only. The access token is decoded only
 * when role claims are configured.
 */
public class RoleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final Logger logger = LoggerFactory.getLogger(RoleOidcUserService.class);

    private final OidcUserService delegate = new OidcUserService();
    private final Roles roles;
    private final boolean readRoles;
    private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();

    /**
     * @param readRoles whether role claims are configured
     */
    public RoleOidcUserService(Roles roles, boolean readRoles) {
        this.roles = roles;
        this.readRoles = readRoles;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser user = delegate.loadUser(request);
        Set<String> idpRoles = Set.of();
        if (readRoles) {
            ClientRegistration registration = request.getClientRegistration();
            Map<String, Object> claims = new LinkedHashMap<>(accessTokenClaims(request));
            claims.putAll(user.getClaims());
            idpRoles = roles.idpRoles(claims, registration.getClientId());
        }
        String nameAttribute = request.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName();
        return new DefaultOidcUser(roles.authorities(user.getEmail(), idpRoles), user.getIdToken(), user.getUserInfo(),
                nameAttribute == null || nameAttribute.isBlank() ? IdTokenClaimNames.SUB : nameAttribute);
    }

    private Map<String, Object> accessTokenClaims(OidcUserRequest request) {
        ClientRegistration registration = request.getClientRegistration();
        ClientRegistration.ProviderDetails provider = registration.getProviderDetails();
        if (provider.getJwkSetUri() == null || provider.getJwkSetUri().isBlank()) {
            return Map.of();
        }
        JwtDecoder decoder = decoders.computeIfAbsent(registration.getRegistrationId(), id -> {
            NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(provider.getJwkSetUri()).build();
            if (provider.getIssuerUri() != null && !provider.getIssuerUri().isBlank()) {
                nimbus.setJwtValidator(JwtValidators.createDefaultWithIssuer(provider.getIssuerUri()));
            }
            return nimbus;
        });
        try {
            return decoder.decode(request.getAccessToken().getTokenValue()).getClaims();
        } catch (JwtException e) {
            logger.debug("Reading roles without the access token, which is not a valid JWT: {}", e.getMessage());
            return Map.of();
        }
    }
}
