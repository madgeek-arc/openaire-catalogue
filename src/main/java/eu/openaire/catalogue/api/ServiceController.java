package eu.openaire.catalogue.api;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Bundles;
import eu.openaire.catalogue.resource.ResourceKind;
import eu.openaire.catalogue.security.AccessRules;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * Service routes of one channel.
 */
public abstract class ServiceController extends RecordController {

    protected ServiceController(String channel, BundleService bundles, AccessRules access) {
        super(ResourceKind.SERVICE, channel, bundles, access);
    }

    @Operation(summary = "Returns every service of a provider, whatever its status or channels, to those who manage "
            + "the provider.")
    @GetMapping("byProvider/{providerId}")
    @PreAuthorize("@access.canManage(authentication, T(eu.openaire.catalogue.resource.ResourceKind).PROVIDER, #providerId)")
    public List<Map<String, Object>> getByProvider(@PathVariable String providerId) {
        return bundles.all(kind, Map.of("resourceOrganisation", providerId)).stream()
                .map(bundle -> Bundles.payload(bundle, kind))
                .toList();
    }
}
