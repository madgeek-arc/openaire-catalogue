package eu.openaire.catalogue.security;

import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "user", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "user")
public class UserController {

    private static final String ROLE_PREFIX = "ROLE_";

    @Operation(summary = "Returns the signed-in user, with their roles without the ROLE_ prefix, such as ADMIN.")
    @GetMapping("info")
    public UserInfo info(@Parameter(hidden = true) Authentication auth) {
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Not signed in");
        }
        UserInfo user = UserInfo.of(auth);
        return new UserInfo(user.sub(), user.email(), user.name(), user.surname(), user.roles().stream()
                .map(role -> role.startsWith(ROLE_PREFIX) ? role.substring(ROLE_PREFIX.length()) : role)
                .toList());
    }
}
