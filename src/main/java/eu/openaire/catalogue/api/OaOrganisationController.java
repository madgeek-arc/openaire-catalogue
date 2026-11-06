package eu.openaire.catalogue.api;

import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.resourcecatalogue.annotations.BrowseCatalogue;
import gr.uoa.di.madgik.resourcecatalogue.domain.OrganisationBundle;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.PublicResourceService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads return the published copy of an organisation, whose id is its PID, without its {@code users}. Writes apply to
 * the internal record and take the internal id.
 */
@RestController
@RequestMapping(path = {"oa/organisations", "oa/providers"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "organisations (OpenAIRE)", description = "Organisations in the OpenAIRE format")
public class OaOrganisationController {

    private final PublicResourceService<OrganisationBundle> publicService;
    private final OrganisationService service;

    public OaOrganisationController(PublicResourceService<OrganisationBundle> publicService,
                                    OrganisationService service) {
        this.publicService = publicService;
        this.service = service;
    }

    @Operation(summary = "Returns the published Organisation with the given id.")
    @GetMapping(path = "{prefix}/{suffix}")
    public ResponseEntity<?> get(@PathVariable String prefix, @PathVariable String suffix) {
        OrganisationBundle bundle = publicService.get(prefix + "/" + suffix);
        if (bundle.isActive()) {
            return ResponseEntity.ok(publicView(bundle));
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
        return ResponseEntity.ok(publicService.getAll(ff).map(OaOrganisationController::publicView));
    }

    @Operation(summary = "Adds a new Organisation.")
    @PostMapping
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> add(@RequestBody LinkedHashMap<String, Object> organisation,
                                 @Parameter(hidden = true) Authentication auth) {
        OrganisationBundle bundle = new OrganisationBundle();
        bundle.setOrganisation(organisation);
        return new ResponseEntity<>(service.add(bundle, auth).getOrganisation(), HttpStatus.CREATED);
    }

    @Operation(summary = "Updates the Organisation with the given internal id.")
    @PutMapping
    @PreAuthorize("@securityService.hasWriteAccess() or @securityService.isOrganisationAdmin(#auth, #organisation['id'])")
    public ResponseEntity<?> update(@RequestBody LinkedHashMap<String, Object> organisation,
                                    @RequestParam(required = false) String comment,
                                    @Parameter(hidden = true) Authentication auth) {
        OrganisationBundle bundle = service.get(organisation.get("id").toString());
        bundle.setOrganisation(organisation);
        return ResponseEntity.ok(service.update(bundle, comment, auth).getOrganisation());
    }

    @Operation(summary = "Deletes the Organisation with the given internal id.")
    @DeleteMapping(path = "{prefix}/{suffix}")
    @PreAuthorize("@securityService.hasWriteAccess()")
    public ResponseEntity<?> delete(@PathVariable String prefix, @PathVariable String suffix) {
        OrganisationBundle bundle = service.get(prefix + "/" + suffix);
        service.delete(bundle);
        return ResponseEntity.ok(bundle.getOrganisation());
    }

    private static LinkedHashMap<String, Object> publicView(OrganisationBundle bundle) {
        LinkedHashMap<String, Object> view = bundle.toPublicMap();
        view.remove("users");
        return view;
    }
}
