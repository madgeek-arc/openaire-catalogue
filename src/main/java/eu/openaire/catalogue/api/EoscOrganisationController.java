package eu.openaire.catalogue.api;

import eu.openaire.catalogue.eosc.EoscOrganisationMapper;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping(path = {"organisations", "providers"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "organisations (EOSC)", description = "Published organisations in the EOSC Beyond format")
public class EoscOrganisationController {

    private final PublicResourceService<OrganisationBundle> service;

    public EoscOrganisationController(PublicResourceService<OrganisationBundle> service) {
        this.service = service;
    }

    @Operation(summary = "Returns the published Organisation with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    public ResponseEntity<?> get(@PathVariable String prefix, @PathVariable String suffix) {
        OrganisationBundle bundle = service.get(prefix + "/" + suffix);
        if (bundle.isActive()) {
            return ResponseEntity.ok(EoscOrganisationMapper.map(bundle.getOrganisation()));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "The specific Organisation is not active"));
    }

    @Operation(summary = "Returns the published Organisations, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping
    public ResponseEntity<Paging<LinkedHashMap<String, Object>>> getAll(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = FacetFilter.from(params);
        ff.addFilter("active", true);
        return ResponseEntity.ok(service.getAll(ff).map(bundle -> EoscOrganisationMapper.map(bundle.getOrganisation())));
    }
}
