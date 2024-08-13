package gr.madgik.catalogue.openaire.resource.controller;


import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import gr.athenarc.catalogue.utils.PagingUtils;
import gr.madgik.catalogue.openaire.domain.Datasource;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.resource.DatasourceBundleService;
import gr.uoa.di.madgik.registry.domain.Paging;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/datasources")
public class DatasourceController {
    private static final Logger logger = LoggerFactory.getLogger(DatasourceController.class);

    private final DatasourceBundleService datasourceBundleService;

    public DatasourceController(DatasourceBundleService datasourceBundleService) {
        this.datasourceBundleService = datasourceBundleService;
    }

    @GetMapping("{id}")
    public Datasource get(@PathVariable("id") String id) {
        return datasourceBundleService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isProviderAdmin(#datasource.resourceOrganisation)")
    public Datasource add(@RequestBody Datasource datasource) {
        return datasourceBundleService.register(datasource); // TODO: change this ??
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isDatasourceProviderAdmin(#datasource)")
    public Datasource update(@PathVariable String id, @RequestBody Datasource datasource) {
        return datasourceBundleService.update(id, new DatasourceBundle(datasource)).getDatasource(); // TODO: change this ??
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isDatasourceProviderAdmin(#id)")
    public void delete(@PathVariable String id) {
        datasourceBundleService.delete(id);
    }

    @Browse
    @GetMapping
    public Paging<Datasource> getAll(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        return datasourceBundleService.getWithEnrichedFacets(PagingUtils.createFacetFilter(allRequestParams)).map(DatasourceBundle::getPayload);
    }

    @PostMapping("validate")
    public boolean validate(@RequestBody Datasource datasource) {
        logger.info("Validating Datasource with id '{}'", datasource.getId());
        return datasourceBundleService.validate(datasource);
    }

    @Operation(summary = "Returns the Datasource (if exists) associated with the specific Service ID, else return null")
    @GetMapping("/byService/{id}")
    public Datasource getDatasourceByServiceId(@PathVariable("id") String id) {
        try {
            return datasourceBundleService.get(id);
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }
}
