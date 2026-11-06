package eu.openaire.catalogue.api;

import eu.openaire.catalogue.eosc.EoscServiceMapper;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
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
@RequestMapping(path = "services", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "services (EOSC)", description = "Published services in the EOSC Beyond format")
public class EoscServiceController {

    private final PublicResourceService<ServiceBundle> service;

    public EoscServiceController(PublicResourceService<ServiceBundle> service) {
        this.service = service;
    }

    @Operation(summary = "Returns the published Service with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    public ResponseEntity<?> get(@PathVariable String prefix, @PathVariable String suffix) {
        ServiceBundle bundle = ServiceChannels.require(service.get(prefix + "/" + suffix), ServiceChannels.NODE);
        if (bundle.isActive()) {
            return ResponseEntity.ok(EoscServiceMapper.map(bundle.getService()));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "The specific Service is not active"));
    }

    @Operation(summary = "Returns the published Services, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping
    public ResponseEntity<Paging<LinkedHashMap<String, Object>>> getAll(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = ServiceChannels.restrict(FacetFilter.from(params), ServiceChannels.NODE);
        return ResponseEntity.ok(service.getAll(ff).map(bundle -> EoscServiceMapper.map(bundle.getService())));
    }
}
