package gr.madgik.catalogue.eosc.resource.controller;

import eu.einfracentral.domain.Service;
import eu.openminted.registry.core.domain.Paging;
import gr.madgik.catalogue.eosc.resource.ServiceBundleService;
import gr.madgik.catalogue.utils.PagingUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

@RestController
@RequestMapping("/v3/services")
public class EOSCServiceController {
    private static final Logger logger = LoggerFactory.getLogger(EOSCServiceController.class);

    private final ServiceBundleService serviceBundleService;

    public EOSCServiceController(ServiceBundleService serviceBundleService) {
        this.serviceBundleService = serviceBundleService;
    }

    @GetMapping("{id}")
    public Service get(@PathVariable("id") String id) {
        return serviceBundleService.get(id);
    }

    @PostMapping
    public Service add(@RequestBody Service service) {
        return serviceBundleService.register(service); // TODO: change this ??
    }

    @PutMapping("{id}")
    public Service update(@PathVariable String id, @RequestBody Service service) {
        return serviceBundleService.update(id, service); // TODO: change this ??
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        serviceBundleService.delete(id);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping
    public Paging<Service> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                  @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        return serviceBundleService.get(PagingUtils.createFacetFilter(allRequestParams));
    }

    @PostMapping(path = "validate")
    public boolean validate(@RequestBody Service service) {
        logger.info("Validating Service with name '{}' and id '{}'", service.getName(), service.getId());
        return serviceBundleService.validate(service);
    }
}
