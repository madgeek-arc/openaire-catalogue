package gr.madgik.catalogue.openaire.resource.controller;

import eu.einfracentral.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.resource.DatasourceBundleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bundles/datasources")
public class DatasourceBundleController {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceBundleController.class);

    private final DatasourceBundleService datasourceBundleService;

    public DatasourceBundleController(DatasourceBundleService datasourceBundleService) {
        this.datasourceBundleService = datasourceBundleService;
    }


    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    public DatasourceBundle verifyProvider(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                           @RequestParam(required = false) String status) {
        DatasourceBundle service = datasourceBundleService.verify(id, status, active);
        logger.info("User updated Service with name '{}' [status: {}] [active: {}]", service.getDatasource().getName(), status, active);
        return service;
    }

    // Activate/Deactivate a Service.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    public DatasourceBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        DatasourceBundle service = datasourceBundleService.activate(id, active);
        logger.info("User updated Service with name '{}' [active: {}]", service.getDatasource().getName(), active);
        return service;
    }
}
