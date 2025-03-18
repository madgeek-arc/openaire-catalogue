package gr.madgik.catalogue.openaire.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
public class CustomLogoutHandler implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomLogoutHandler.class);

    private final ApplicationProperties applicationProperties;

    public CustomLogoutHandler(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Cookie cookie = new Cookie(applicationProperties.getCookie().getName(), ((OidcUser) authentication.getPrincipal()).getIdToken().getTokenValue());
        cookie.setMaxAge(0);
        cookie.setPath(applicationProperties.getCookie().getPath());
        cookie.setDomain(applicationProperties.getCookie().getDomain());
        cookie.setSecure(applicationProperties.getCookie().isSecure());

        if (logger.isDebugEnabled()) {
            logger.debug("Removing '{}' cookie", applicationProperties.getCookie().getName());
        }
        response.addCookie(cookie);
    }
}
