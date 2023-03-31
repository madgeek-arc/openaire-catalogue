package gr.madgik.catalogue.openaire.resource.controller;

import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.catalogue.utils.PagingUtils;
import gr.madgik.catalogue.dto.BulkOperation;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.resource.ServiceBundleService;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bundles/services")
public class ServiceBundleController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBundleController.class);

    private final ServiceBundleService serviceBundleService;
    private final RegistryCoreRepository<ServiceBundle, String> serviceRepository;

    public ServiceBundleController(ServiceBundleService serviceBundleService,
                                   RegistryCoreRepository<ServiceBundle, String> serviceRepository) {
        super();
        this.serviceBundleService = serviceBundleService;
        this.serviceRepository = serviceRepository;
    }


    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public Paging<ServiceBundle> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        return serviceBundleService.getWithEnrichedFacets(PagingUtils.createFacetFilter(allRequestParams));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ServiceBundle get(@PathVariable("id") String id) {
        return serviceRepository.findById(id).orElse(null);
    }

    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ServiceBundle verifyProvider(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                        @RequestParam(required = false) String status) {
        ServiceBundle service = serviceBundleService.verify(id, status, active);
        logger.info("User updated Service with name '{}' [status: {}] [active: {}]", service.getPayload().getName(), status, active);
        return service;
    }

    // Activate/Deactivate a Service.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('ADMIN')")
    public ServiceBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        ServiceBundle service = serviceBundleService.activate(id, active);
        logger.info("User updated Service with name '{}' [active: {}]", service.getPayload().getName(), active);
        return service;
    }

    @PostMapping("bulk")
    @PreAuthorize("hasAuthority('ADMIN')")
    public BulkOperation<ServiceBundle> addAll(@RequestBody List<ServiceBundle> bundles) {
        BulkOperation<ServiceBundle> services = new BulkOperation<>();
        for (ServiceBundle bundle : bundles) {
            try {
                services.getSuccessful().add(serviceRepository.create(bundle));
            } catch (Exception e) {
                services.getFailed().add(bundle);
            }
        }
        return services;
    }

    @PutMapping("bulk")
    @PreAuthorize("hasAuthority('ADMIN')")
    public BulkOperation<ServiceBundle> updateAll(@RequestBody List<ServiceBundle> bundles) {
        BulkOperation<ServiceBundle> services = new BulkOperation<>();
        for (ServiceBundle bundle : bundles) {
            try {
                services.getSuccessful().add(serviceRepository.update(bundle.getId(), bundle));
            } catch (Exception e) {
                services.getFailed().add(bundle);
            }
        }
        return services;
    }
}
