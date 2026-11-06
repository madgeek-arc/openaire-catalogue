package eu.openaire.catalogue.api;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.ServiceBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads return the published copy of a service, whose id is its PID. Writes apply to the internal record and take the
 * internal id.
 */
@RestController
@RequestMapping(path = "oa/services", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "services (OpenAIRE)", description = "Services in the OpenAIRE format")
public class OaServiceController {

    private final PublicResourceService<ServiceBundle> publicService;
    private final ServiceService service;

    public OaServiceController(PublicResourceService<ServiceBundle> publicService, ServiceService service) {
        this.publicService = publicService;
        this.service = service;
    }

    @Operation(summary = "Returns the published Service with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    public ResponseEntity<?> get(@PathVariable String prefix, @PathVariable String suffix) {
        ServiceBundle bundle = ServiceChannels.require(publicService.get(prefix + "/" + suffix), ServiceChannels.OA);
        if (bundle.isActive()) {
            return ResponseEntity.ok(bundle.toPublicMap());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "The specific Service is not active"));
    }

    @Operation(summary = "Returns the published Services, based on a set of filters.")
    @BrowseParameters
    @BrowseCatalogue
    @GetMapping
    public ResponseEntity<Paging<LinkedHashMap<String, Object>>> getAll(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter ff = ServiceChannels.restrict(FacetFilter.from(params), ServiceChannels.OA);
        return ResponseEntity.ok(publicService.getAll(ff).map(Bundle::toPublicMap));
    }

    @Operation(summary = "Adds a new Service.")
    @PostMapping
    @PreAuthorize("isAuthenticated() and (@securityService.hasWriteAccess() or " +
            "@securityService.providerCanAddResources(#auth, #serviceMap, null))")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> serviceMap,
                                 @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = new ServiceBundle();
        bundle.setService(serviceMap);
        return new ResponseEntity<>(service.add(bundle, auth).getService(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the Service with the given internal id.")
    @PutMapping
    @PreAuthorize("@securityService.hasWriteAccess() or @securityService.isResourceAdmin(#auth, #serviceMap['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> serviceMap,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = service.get(serviceMap.get("id").toString());
        bundle.setService(serviceMap);
        return ResponseEntity.ok(service.update(bundle, comment, auth).getService());
    }

    @Operation(summary = "Adds the Service with the given internal id to a channel, or removes it from one.")
    @PatchMapping(path = "setChannels/{prefix}/{suffix}")
    @PreAuthorize("@securityService.hasWriteAccess()")
    public ResponseEntity<List<String>> setChannel(@PathVariable String prefix, @PathVariable String suffix,
                                                   @RequestParam String channel, @RequestParam boolean enabled,
                                                   @Parameter(hidden = true) Authentication auth) {
        String id = prefix + "/" + suffix;
        List<String> channels = ServiceChannels.change(service.get(id), channel, enabled);
        return ResponseEntity.ok(ServiceChannels.of(service.setProperty(id, ServiceChannels.PROPERTY, channels, auth)));
    }

    @Operation(summary = "Deletes the Service with the given internal id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("@securityService.hasWriteAccess() or @securityService.isResourceAdmin(#auth, #prefix+'/'+#suffix)")
    public ResponseEntity<?> delete(@PathVariable String prefix, @PathVariable String suffix,
                                    @SuppressWarnings("unused") @Parameter(hidden = true) Authentication auth) {
        ServiceBundle bundle = service.get(prefix + "/" + suffix);
        service.delete(bundle);
        return ResponseEntity.ok(bundle.getService());
    }
}
