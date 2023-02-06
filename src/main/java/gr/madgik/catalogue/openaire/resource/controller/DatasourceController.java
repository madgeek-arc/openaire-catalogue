package gr.madgik.catalogue.openaire.resource.controller;


import eu.openminted.registry.core.domain.Paging;
import gr.madgik.catalogue.openaire.domain.Datasource;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.resource.DatasourceBundleService;
import gr.madgik.catalogue.utils.PagingUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

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
    public Datasource add(@RequestBody Datasource datasource) {
        return datasourceBundleService.register(datasource); // TODO: change this ??
    }

    @PutMapping("{id}")
    public Datasource update(@PathVariable String id, @RequestBody Datasource datasource) {
        return datasourceBundleService.update(id, new DatasourceBundle(datasource)).getDatasource(); // TODO: change this ??
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        datasourceBundleService.delete(id);
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataTypeClass = String.class, paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataTypeClass = String.class, paramType = "query")
    })
    @GetMapping
    public Paging<Datasource> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                     @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        return datasourceBundleService.getWithEnrichedFacets(PagingUtils.createFacetFilter(allRequestParams)).map(DatasourceBundle::getPayload);
    }

//    @PostMapping(path = "search")
//    public FacetedPage<Datasource> search(@RequestBody Map<String, Object> filters, @RequestParam(required = false, name = "catalogue_id") String catalogueIds, Pageable pageable) {
//        if (catalogueIds != null && !catalogueIds.equalsIgnoreCase("all")) {
//            filters.putIfAbsent("catalogueId", catalogueIds);
//        }
//        return datasourceBundleService.search(filters, pageable);
//    }

    @PostMapping(path = "validate")
    public boolean validate(@RequestBody Datasource datasource) {
        logger.info("Validating Datasource with name '{}' and id '{}'", datasource.getName(), datasource.getId());
        return datasourceBundleService.validate(datasource);
    }
}
