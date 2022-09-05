package gr.madgik.catalogue.eosc.resource.controller;

import eu.einfracentral.domain.ServiceBundle;
import gr.madgik.catalogue.eosc.resource.ServiceBundleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v3/bundles/datasources")
public class DatasourceBundleController {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceBundleController.class);

    private final ServiceBundleService serviceBundleService;

    public DatasourceBundleController(ServiceBundleService serviceBundleService) {
        this.serviceBundleService = serviceBundleService;
    }


    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ServiceBundle verifyProvider(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                         @RequestParam(required = false) String status) {
        ServiceBundle service = serviceBundleService.verify(id, status, active);
        logger.info("User updated Service with name '{}' [status: {}] [active: {}]", service.getService().getName(), status, active);
        return service;
    }

    // Activate/Deactivate a Service.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ServiceBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        ServiceBundle service = serviceBundleService.activate(id, active);
        logger.info("User updated Service with name '{}' [active: {}]", service.getService().getName(), active);
        return service;
    }
}
