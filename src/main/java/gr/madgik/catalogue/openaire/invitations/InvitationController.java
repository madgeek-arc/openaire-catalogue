package gr.madgik.catalogue.openaire.invitations;

import gr.madgik.catalogue.domain.User;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "invitations", produces = MediaType.APPLICATION_JSON_VALUE)
public class InvitationController {

    private final InvitationRepository invitationRepository;
    private final InvitationService invitationService;

    public InvitationController(InvitationRepository invitationRepository, InvitationService invitationService) {
        this.invitationRepository = invitationRepository;
        this.invitationService = invitationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<Invitation> getAll() {
        return invitationRepository.findAll();
    }

    @PostMapping(produces = MediaType.TEXT_HTML_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public String create(@RequestParam String email, @Parameter(hidden = true) Authentication authentication) {
        return invitationService.create(User.of(authentication), email);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable String id) {
        invitationRepository.deleteById(UUID.fromString(id));
    }

}
