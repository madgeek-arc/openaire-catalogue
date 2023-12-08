package gr.madgik.catalogue.openaire.utils;

import eu.einfracentral.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class UserUtils {

    private static final Logger logger = LoggerFactory.getLogger(UserUtils.class);

    public static User getUserFromAuthentication(Authentication authentication) {
        logger.trace("Creating User from Authentication\n{}", authentication);
        User user = new User();
        if (authentication == null) {
            throw new InsufficientAuthenticationException("You are not authenticated, please log in.");
        } else if (authentication.getPrincipal() instanceof OidcUser principal) {
                user.setId(principal.getSubject());
                user.setEmail(principal.getEmail());
                user.setName(principal.getGivenName());
                user.setSurname(principal.getFamilyName());
        } else if (authentication instanceof OAuth2AuthenticationToken token) {
            user.setId(token.getPrincipal().getAttribute("sub"));
            if (user.getId() == null) {
                user.setId("");
            }
            user.setEmail(token.getPrincipal().getAttribute("email"));
            if (user.getEmail() == null) {
                user.setEmail("");
            }
            user.setName(token.getPrincipal().getAttribute("given_name"));
            user.setSurname(token.getPrincipal().getAttribute("family_name"));
        } else {
            throw new InsufficientAuthenticationException("Could not create user. Insufficient user authentication");
        }
        logger.debug("User from Authentication: {}", user);
        return user;
    }

    private UserUtils() {
    }
}
