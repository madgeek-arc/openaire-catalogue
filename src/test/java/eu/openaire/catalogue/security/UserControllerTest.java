package eu.openaire.catalogue.security;

import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserControllerTest {

    @Test
    void returnsRolesWithoutPrefix() {
        var authorities = AuthorityUtils.createAuthorityList("ROLE_USER", "ROLE_ADMIN", "ROLE_ADMIN_OA", "OIDC_USER");
        OidcIdToken token = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "sub", "email", "admin@example.org", "given_name", "Ad", "family_name", "Min"));
        var auth = new OAuth2AuthenticationToken(new DefaultOidcUser(authorities, token), authorities, "openaire");

        UserInfo info = new UserController().info(auth);

        assertEquals(List.of("OIDC_USER", "ADMIN", "ADMIN_OA", "USER"), info.roles());
        assertEquals("admin@example.org", info.email());
    }
}
