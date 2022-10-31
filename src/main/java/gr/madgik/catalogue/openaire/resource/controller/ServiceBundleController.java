package gr.madgik.catalogue.openaire.resource.controller;

import gr.madgik.catalogue.dto.BulkOperation;
import gr.madgik.catalogue.openaire.OpenAIREServiceBundle;
import gr.madgik.catalogue.openaire.resource.ServiceBundleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bundles/services")
public class ServiceBundleController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBundleController.class);

    private final ServiceBundleService serviceBundleService;

    public ServiceBundleController(ServiceBundleService serviceBundleService) {
        this.serviceBundleService = serviceBundleService;
    }


    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    public OpenAIREServiceBundle verifyProvider(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                                @RequestParam(required = false) String status) {
        OpenAIREServiceBundle service = serviceBundleService.verify(id, status, active);
        logger.info("User updated Service with name '{}' [status: {}] [active: {}]", service.getPayload().getName(), status, active);
        return service;
    }

    // Activate/Deactivate a Service.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    public OpenAIREServiceBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        OpenAIREServiceBundle service = serviceBundleService.activate(id, active);
        logger.info("User updated Service with name '{}' [active: {}]", service.getPayload().getName(), active);
        return service;
    }

    @PostMapping("bulk")
    public BulkOperation<OpenAIREServiceBundle> addAll(@RequestBody List<OpenAIREServiceBundle> bundles) {
        BulkOperation<OpenAIREServiceBundle> services = new BulkOperation<>();
        for (OpenAIREServiceBundle bundle : bundles) {
            try {
                services.getSuccessful().add(serviceBundleService.create(bundle)); // TODO: change this ??
            } catch (Exception e) {
                services.getFailed().add(bundle);
            }
        }
        return services;
    }
}
