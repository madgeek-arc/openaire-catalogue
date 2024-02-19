package gr.madgik.catalogue.openaire.resource.controller;

import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.utils.PagingUtils;
import gr.madgik.catalogue.dto.BulkOperation;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.resource.ServiceBundleService;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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


    @GetMapping
    @Browse
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public Paging<ServiceBundle> getAll(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        return serviceBundleService.getWithEnrichedFacets(PagingUtils.createFacetFilter(allRequestParams));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ServiceBundle get(@PathVariable("id") String id) {
        return serviceRepository.findById(id).orElse(null);
    }

    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ServiceBundle verify(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                @RequestParam(required = false) String status) {
        ServiceBundle service = serviceBundleService.verify(id, status, active);
        logger.info("User updated Service with name '{}' [status: {}] [active: {}]", service.getPayload().getName(), status, active);
        return service;
    }

    // Activate/Deactivate a Service.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ServiceBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        ServiceBundle service = serviceBundleService.activate(id, active);
        logger.info("User updated Service with name '{}' [active: {}]", service.getPayload().getName(), active);
        return service;
    }

    @PostMapping("bulk")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
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
