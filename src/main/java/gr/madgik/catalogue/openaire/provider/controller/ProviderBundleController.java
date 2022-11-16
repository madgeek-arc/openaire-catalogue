package gr.madgik.catalogue.openaire.provider.controller;

import eu.einfracentral.domain.ProviderBundle;
import gr.madgik.catalogue.openaire.provider.ProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bundles/providers")
public class ProviderBundleController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderBundleController.class);

    private final ProviderService providerService;

    public ProviderBundleController(ProviderService providerService) {
        this.providerService = providerService;
    }


    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ProviderBundle verifyProvider(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                         @RequestParam(required = false) String status) {
        ProviderBundle provider = providerService.verify(id, status, active);
        logger.info("User updated Provider with name '{}' [status: {}] [active: {}]", provider.getProvider().getName(), status, active);
        return provider;
    }

    // Activate/Deactivate a Provider.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ProviderBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        ProviderBundle provider = providerService.activate(id, active);
        logger.info("User updated Provider with name '{}' [active: {}]", provider.getProvider().getName(), active);
        return provider;
    }

    @PostMapping("bulk")
    public void addAll(@RequestBody List<ProviderBundle> providers) {
        for (ProviderBundle providerBundle : providers) {
            providerService.create(providerBundle); // TODO: change this ??
        }
//        return
    }
}
