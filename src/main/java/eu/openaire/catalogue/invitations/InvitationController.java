package eu.openaire.catalogue.invitations;

import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "invitations", produces = MediaType.APPLICATION_JSON_VALUE)
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping(produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED)
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    public String create(@RequestParam String email, @Parameter(hidden = true) Authentication authentication) {
        return invitationService.create(UserInfo.of(authentication).email(), email);
    }

}
