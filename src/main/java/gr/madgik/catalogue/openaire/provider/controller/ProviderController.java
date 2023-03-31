package gr.madgik.catalogue.openaire.provider.controller;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.catalogue.utils.PagingUtils;
import gr.madgik.catalogue.domain.User;
import gr.madgik.catalogue.openaire.provider.ProviderService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/providers")
public class ProviderController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderController.class);

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping("{id}")
    public Provider get(@PathVariable("id") String id) {
        return providerService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public Provider add(@RequestBody Provider provider) {
        return providerService.register(provider); // TODO: change this ??
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN') or isProviderAdmin(#provider)")
    public Provider update(@PathVariable String id, @RequestBody Provider provider) {
        return providerService.update(id, new ProviderBundle(provider)).getProvider(); // TODO: change this ??
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public void delete(@PathVariable String id) {
        providerService.delete(id);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping
    public Paging<Provider> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        return providerService.getWithEnrichedFacets(PagingUtils.createFacetFilter(allRequestParams)).map(ProviderBundle::getPayload);
    }

    @GetMapping(path = "my")
    public List<Provider> getMy(@ApiIgnore Authentication authentication) {
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
