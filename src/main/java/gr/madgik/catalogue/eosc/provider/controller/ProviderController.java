package gr.madgik.catalogue.eosc.provider.controller;

import eu.einfracentral.domain.Provider;
import eu.openminted.registry.core.domain.Paging;
import gr.madgik.catalogue.eosc.provider.ProviderService;
import gr.madgik.catalogue.utils.PagingUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

@RestController
@RequestMapping("/v3/providers")
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
    public Provider add(@RequestBody Provider provider) {
        return providerService.register(provider); // TODO: change this ??
    }

    @PutMapping("{id}")
    public Provider update(@PathVariable String id, @RequestBody Provider provider) {
        return providerService.update(id, provider); // TODO: change this ??
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        providerService.delete(id);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping
    public Paging<Provider> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                   @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        return providerService.get(PagingUtils.createFacetFilter(allRequestParams));
    }

    @PostMapping(path = "validate")
    public boolean validate(@RequestBody Provider provider) {
        logger.info("Validating Provider with name '{}' and id '{}'", provider.getName(), provider.getId());
        return providerService.validate(provider);
    }
}
