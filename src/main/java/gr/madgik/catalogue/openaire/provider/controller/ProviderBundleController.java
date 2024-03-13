package gr.madgik.catalogue.openaire.provider.controller;

import eu.einfracentral.domain.ProviderBundle;
import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.utils.PagingUtils;
import gr.madgik.catalogue.openaire.provider.ProviderService;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bundles/providers")
public class ProviderBundleController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderBundleController.class);

    private final ProviderService providerService;

    public ProviderBundleController(ProviderService providerService) {
        this.providerService = providerService;
    }


    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ProviderBundle verify(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                 @RequestParam(required = false) String status) {
        ProviderBundle provider = providerService.verify(id, status, active);
        logger.info("User updated Provider with name '{}' [status: {}] [active: {}]", provider.getProvider().getName(), status, active);
        return provider;
    }

    // Activate/Deactivate a Provider.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ProviderBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        ProviderBundle provider = providerService.activate(id, active);
        logger.info("User updated Provider with name '{}' [active: {}]", provider.getProvider().getName(), active);
        return provider;
    }

    @PostMapping("bulk")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public void addAll(@RequestBody List<ProviderBundle> providers) {
        for (ProviderBundle providerBundle : providers) {
            providerService.create(providerBundle); // TODO: change this ??
        }
//        return
    }

    @Browse
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public Paging<ProviderBundle> getAll(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        return providerService.getWithEnrichedFacets(PagingUtils.createFacetFilter(allRequestParams));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ProviderBundle get(@PathVariable("id") String id) {
        return providerService.getBundle(id);
    }
}
