package gr.madgik.catalogue.openaire.resource.controller;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.ResourceBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.domain.Resource;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.service.FacetLabelService;
import gr.madgik.catalogue.utils.PagingUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.AbstractMap;
import java.util.Map;

@RestController
@RequestMapping("catalogue-resources")
public class CatalogueResourcesController {

    private final GenericItemService genericItemService;
    private final FacetLabelService facetLabelService;


    public CatalogueResourcesController(GenericItemService genericItemService,
                                        FacetLabelService facetLabelService) {
        this.genericItemService = genericItemService;
        this.facetLabelService = facetLabelService;
    }

    @GetMapping("{id}")
    public <T extends Bundle<?>> Object get(@PathVariable("id") String id) {
        T bundle = genericItemService.get("resources", id);
        return bundle.getPayload();
    }

    @GetMapping("{id}/resourceType")
    public Map.Entry<String, String> getResourceType(@PathVariable("id") String id) {
        Resource resource = genericItemService.searchResource("resources", id, true);
        return new AbstractMap.SimpleEntry<>("resourceType", resource.getResourceTypeName());
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping
    public Paging<?> getCatalogueResources(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter filter = PagingUtils.createFacetFilter(allRequestParams);
        filter.setResourceType("resources");
        Paging<?> paging = genericItemService.getResults(filter).map(r -> ((ResourceBundle<?>) r).getPayload());
        paging.setFacets(facetLabelService.createLabels(paging.getFacets()));
        return paging;
    }


}
