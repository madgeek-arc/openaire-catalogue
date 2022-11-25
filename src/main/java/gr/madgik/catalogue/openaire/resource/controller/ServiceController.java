package gr.madgik.catalogue.openaire.resource.controller;

import eu.einfracentral.domain.Bundle;
import eu.openminted.registry.core.domain.Paging;
import gr.madgik.catalogue.openaire.domain.Service;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.resource.ServiceBundleService;
import gr.madgik.catalogue.utils.PagingUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;

@RestController
@RequestMapping(value = "/services", produces = APPLICATION_JSON)
public class ServiceController {
    private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);

    private final ServiceBundleService serviceBundleService;

    public ServiceController(ServiceBundleService serviceBundleService) {
        this.serviceBundleService = serviceBundleService;
    }

    @GetMapping("{id}")
    public Service get(@PathVariable("id") String id) {
        return serviceBundleService.get(id);
    }

    @PostMapping
    public Service add(@RequestBody Service service) {
        return serviceBundleService.register(service); // TODO: change this ??
    }

    @PutMapping("{id}")
    public Service update(@PathVariable String id, @RequestBody Service service) {
        return serviceBundleService.update(id, new ServiceBundle(service)).getService(); // TODO: change this ??
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        serviceBundleService.delete(id);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping
    public Paging<Service> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        return serviceBundleService.getWithEnrichedFacets(PagingUtils.createFacetFilter(allRequestParams)).map(Bundle::getPayload);
    }

    @PostMapping(path = "validate")
    public boolean validate(@RequestBody Service service) {
        logger.info("Validating Service with name '{}' and id '{}'", service.getName(), service.getId());
        return serviceBundleService.validate(service);
    }

    @GetMapping(path = "/by/{field}")
    public Map<String, List<Service>> by(@PathVariable String field, @RequestParam("vocabularyType") String type) {
        logger.info("Requesting Services by [vocabulary={}]", type);
        return serviceBundleService.getByVocabulary(field, type);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", value = "Comma-separated list of Resource ids", dataType = "string", paramType = "path")
    })
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @GetMapping(path = "ids/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Service>> getSomeServices(@PathVariable("ids") String[] ids, @ApiIgnore Authentication auth) {
        return ResponseEntity.ok(serviceBundleService.getByIds(ids));
    }
}
