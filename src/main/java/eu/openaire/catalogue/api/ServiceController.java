package eu.openaire.catalogue.api;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.RecordGroups;
import eu.openaire.catalogue.resource.ResourceKind;
import eu.openaire.catalogue.security.AccessRules;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Service routes of one channel.
 */
public abstract class ServiceController extends RecordController {

    private static final String RESOURCE_ORGANISATION = "resourceOrganisation";
    private static final String RESOURCE_PROVIDERS = "resourceProviders";
    /**
     * Global reviewers, or the users of the provider; reviewing a provider on one channel does not reveal its services
     * on other channels.
     */
    private static final String PROVIDER_USER_OR_REVIEWER = "@access.isGlobalReviewer(authentication) or "
            + "@access.managesAsUser(authentication, T(eu.openaire.catalogue.resource.ResourceKind).PROVIDER, #providerId)";

    protected ServiceController(String channel, BundleService bundles, RecordGroups groups, AccessRules access) {
        super(ResourceKind.SERVICE, channel, bundles, groups, access);
    }

    /**
     * Lists services by name.
     */
    @Override
    protected void defaultOrder(FacetFilter filter) {
        filter.addOrderBy("name", "asc");
    }

    @Operation(summary = "Returns the bundles of the services a provider owns ('resourceOrganisation'), whatever their "
            + "status or channels, based on a set of filters. For reviewers and the users of the provider.")
    @BrowseParameters
    @GetMapping("bundles/byOrganisation/{providerId}")
    @PreAuthorize(PROVIDER_USER_OR_REVIEWER)
    public Paging<Map<String, Object>> getBundlesByOrganisation(
            @PathVariable String providerId,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        return providerBundles(RESOURCE_ORGANISATION, providerId, params);
    }

    @Operation(summary = "Returns the bundles of the services that list a provider among their 'resourceProviders', "
            + "whatever their status or channels, based on a set of filters. A service that also lists its owner there "
            + "is returned by 'bundles/byOrganisation' too. For reviewers and the users of the provider.")
    @BrowseParameters
    @GetMapping("bundles/byProvider/{providerId}")
    @PreAuthorize(PROVIDER_USER_OR_REVIEWER)
    public Paging<Map<String, Object>> getBundlesByProvider(
            @PathVariable String providerId,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        return providerBundles(RESOURCE_PROVIDERS, providerId, params);
    }

    /**
     * Searches the services whose field names the provider, overriding any value the client sent for the field. The
     * filter is fixed, so the facet of the field does not count services of other providers.
     */
    private Paging<Map<String, Object>> providerBundles(String field, String providerId,
                                                        MultiValueMap<String, Object> params) {
        FacetFilter filter = FacetFilter.from(params);
        filter.addFilter(field, providerId);
        if (filter.getOrderBy() == null || filter.getOrderBy().isEmpty()) {
            defaultOrder(filter);
        }
        return bundles.search(kind, filter);
    }
}
