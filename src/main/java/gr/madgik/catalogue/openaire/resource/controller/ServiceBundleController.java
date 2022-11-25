package gr.madgik.catalogue.openaire.resource.controller;

import eu.openminted.registry.core.domain.Paging;
import gr.madgik.catalogue.dto.BulkOperation;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.resource.ServiceBundleService;
import gr.madgik.catalogue.utils.PagingUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bundles/services")
public class ServiceBundleController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBundleController.class);

    private final ServiceBundleService serviceBundleService;

    public ServiceBundleController(ServiceBundleService serviceBundleService) {
        super();
        this.serviceBundleService = serviceBundleService;
    }


    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping
    public Paging<ServiceBundle> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        return serviceBundleService.getWithEnrichedFacets(PagingUtils.createFacetFilter(allRequestParams));
    }


    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ServiceBundle verifyProvider(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                        @RequestParam(required = false) String status) {
        ServiceBundle service = serviceBundleService.verify(id, status, active);
        logger.info("User updated Service with name '{}' [status: {}] [active: {}]", service.getPayload().getName(), status, active);
        return service;
    }

    // Activate/Deactivate a Service.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ServiceBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        ServiceBundle service = serviceBundleService.activate(id, active);
        logger.info("User updated Service with name '{}' [active: {}]", service.getPayload().getName(), active);
        return service;
    }

    @PostMapping("bulk")
    public BulkOperation<ServiceBundle> addAll(@RequestBody List<ServiceBundle> bundles) {
        BulkOperation<ServiceBundle> services = new BulkOperation<>();
        for (ServiceBundle bundle : bundles) {
            try {
                bundle.setStatus("approved resource");
                bundle.setActive(true);
                services.getSuccessful().add(serviceBundleService.create(bundle)); // TODO: change this ??
            } catch (Exception e) {
                services.getFailed().add(bundle);
            }
        }
        return services;
    }
}
