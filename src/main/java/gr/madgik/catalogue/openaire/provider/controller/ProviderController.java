package gr.madgik.catalogue.openaire.provider.controller;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.utils.PagingUtils;
import gr.madgik.catalogue.domain.User;
import gr.madgik.catalogue.openaire.invitations.Invitation;
import gr.madgik.catalogue.openaire.invitations.InvitationService;
import gr.madgik.catalogue.openaire.provider.ProviderService;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/providers")
public class ProviderController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderController.class);

    private final ProviderService providerService;
    private final InvitationService invitationService;

    public ProviderController(ProviderService providerService, InvitationService invitationService) {
        this.providerService = providerService;
        this.invitationService = invitationService;
    }

    @GetMapping("{id}")
    public Provider get(@PathVariable("id") String id) {
        return providerService.get(id);
    }

    @PostMapping
    @ResponseStatus(code = HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or hasProviderInvitation(#invitationToken)")
    public Provider add(@RequestBody Provider provider, @RequestParam(name = "invitation", required = false) String invitationToken) {
        Invitation invitation = invitationService.validateAndConstructInvitation(invitationToken);
        invitationService.accept(invitation);
        return providerService.register(provider);
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isProviderAdmin(#provider)")
    public Provider update(@PathVariable String id, @RequestBody Provider provider) {
        return providerService.update(id, new ProviderBundle(provider)).getProvider(); // TODO: change this ??
    }

    @DeleteMapping("{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable String id) {
        providerService.delete(id);
    }

    @Browse
    @GetMapping
    public Paging<Provider> getAll(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        return providerService.getWithEnrichedFacets(PagingUtils.createFacetFilter(allRequestParams)).map(ProviderBundle::getPayload);
    }

    @GetMapping(path = "my")
    public List<Provider> getMy(@Parameter(hidden = true) Authentication authentication) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter("users", User.of(authentication).getEmail());
        return providerService.get(filter).map(ProviderBundle::getPayload).getResults();
    }

    @PostMapping(path = "validate")
    public boolean validate(@RequestBody Provider provider) {
        logger.info("Validating Provider with name '{}' and id '{}'", provider.getName(), provider.getId());
        return providerService.validate(provider);
    }
}
