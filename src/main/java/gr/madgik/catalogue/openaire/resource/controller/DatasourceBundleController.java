package gr.madgik.catalogue.openaire.resource.controller;

import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.dto.BulkOperation;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.madgik.catalogue.openaire.resource.DatasourceBundleService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bundles/datasources")
public class DatasourceBundleController {

    private static final Logger logger = LoggerFactory.getLogger(DatasourceBundleController.class);

    private final DatasourceBundleService datasourceBundleService;
    private final RegistryCoreRepository<DatasourceBundle, String> datasourceRepository;

    public DatasourceBundleController(DatasourceBundleService datasourceBundleService,
                                      RegistryCoreRepository<DatasourceBundle, String> datasourceRepository) {
        this.datasourceBundleService = datasourceBundleService;
        this.datasourceRepository = datasourceRepository;
    }


    @BrowseParameters
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public Paging<DatasourceBundle> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        return datasourceRepository.get(FacetFilter.from(allRequestParams));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isDatasourceProviderAdmin(#id)")
    public DatasourceBundle get(@PathVariable("id") String id) {
        return datasourceBundleService.getBundle(id);
    }

    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public DatasourceBundle verify(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                   @RequestParam(required = false) String status) {
        DatasourceBundle datasource = datasourceBundleService.verify(id, status, active);
        logger.info("User updated Datasource with id '{}' [status: {}] [active: {}]", datasource.getPayload().getId(), status, active);
        return datasource;
    }

    // Activate/Deactivate a Datasource.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public DatasourceBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        DatasourceBundle datasource = datasourceBundleService.activate(id, active);
        logger.info("User updated Datasource with id '{}' [active: {}]", datasource.getPayload().getId(), active);
        return datasource;
    }

    @PostMapping("bulk")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public BulkOperation<DatasourceBundle> addAll(@RequestBody List<DatasourceBundle> bundles) {
        BulkOperation<DatasourceBundle> datasources = new BulkOperation<>();
        for (DatasourceBundle bundle : bundles) {
            try {
                datasources.getSuccessful().add(datasourceRepository.create(bundle));
            } catch (Exception e) {
                datasources.getFailed().add(bundle);
            }
        }
        return datasources;
    }

    @PutMapping("bulk")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public BulkOperation<DatasourceBundle> updateAll(@RequestBody List<DatasourceBundle> bundles) {
        BulkOperation<DatasourceBundle> datasources = new BulkOperation<>();
        for (DatasourceBundle bundle : bundles) {
            try {
                datasources.getSuccessful().add(datasourceRepository.update(bundle.getId(), bundle));
            } catch (Exception e) {
                datasources.getFailed().add(bundle);
            }
        }
        return datasources;
    }
}
