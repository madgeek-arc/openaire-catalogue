package gr.madgik.catalogue.openaire.resource.controller;

import eu.einfracentral.domain.Vocabulary;
import gr.madgik.catalogue.dto.FacetedPage;
import gr.madgik.catalogue.openaire.OpenAIREService;
import gr.madgik.catalogue.openaire.dto.ResourceSnippet;
import gr.madgik.catalogue.openaire.resource.ServiceBundleService;
import gr.madgik.catalogue.utils.PagingUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public OpenAIREService get(@PathVariable("id") String id) {
        return serviceBundleService.get(id);
    }

    @PostMapping
    public OpenAIREService add(@RequestBody OpenAIREService service) {
        return serviceBundleService.register(service); // TODO: change this ??
    }

    @PutMapping("{id}")
    public OpenAIREService update(@PathVariable String id, @RequestBody OpenAIREService service) {
        return serviceBundleService.update(id, service); // TODO: change this ??
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
    public Page<OpenAIREService> getAll(@ApiIgnore @RequestParam Map<String, Object> allRequestParams,
                                @RequestParam(defaultValue = "eosc", name = "catalogue_id") String catalogueIds) {
        allRequestParams.putIfAbsent("catalogue_id", catalogueIds);
        if (catalogueIds != null && catalogueIds.equals("all")) {
            allRequestParams.remove("catalogue_id");
        }
        return serviceBundleService.get(PagingUtils.toPageable(PagingUtils.createFacetFilter(allRequestParams)));
    }

    @PostMapping(path = "search")
    public FacetedPage<OpenAIREService> search(@RequestBody Map<String, Object> filters, @RequestParam(required = false, name = "catalogue_id") String catalogueIds,
                                               @RequestParam(value = "page", defaultValue = "0") int page,
                                               @RequestParam(value = "size", defaultValue = "10") int size,
                                               @RequestParam(value = "sort", defaultValue = "name") String sort,
                                               @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction) {
        Sort.Order order = new Sort.Order(direction, sort.startsWith("payload.") ? sort : "payload." + sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(order));
        if (catalogueIds != null && !catalogueIds.equalsIgnoreCase("all")) {
            filters.putIfAbsent("catalogueId", catalogueIds);
        }
        return serviceBundleService.search(filters, pageable);
    }

    @GetMapping(path = "snippets/search")
    public FacetedPage<OpenAIREService> searchSnippets(@RequestParam(required = false, name = "catalogue_id") String catalogueIds,
                                                       @RequestParam(value = "page", defaultValue = "0") int page,
                                                       @RequestParam(value = "size", defaultValue = "10") int size,
                                                       @RequestParam(value = "sort", defaultValue = "name") String sort,
                                                       @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction,
                                                       @RequestParam(value = "field", required = false) String field,
                                                       @RequestParam(value = "keyword", required = false) String keyword,
                                                       @ApiIgnore @RequestParam Map<String, Object> filters) {
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        filters.remove("direction");
        filters.remove("field");
        filters.remove("keyword");
        Sort.Order order = new Sort.Order(direction, sort.startsWith("payload.") ? sort : "payload." + sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(order));
        if (catalogueIds != null && !catalogueIds.equalsIgnoreCase("all")) {
            filters.putIfAbsent("catalogueId", catalogueIds);
        }
        String[] fields = null /*{"payload.id","payload.name","payload.description","payload.tagline","payload.logo","payload.extras.image","payload.extras.longImage","payload.extras.paymentCategories.paymentTitle"}*/;
        return serviceBundleService.searchSnippets(filters, field, keyword, pageable, fields);
    }

    @PostMapping(path = "validate")
    public boolean validate(@RequestBody OpenAIREService service) {
        logger.info("Validating Service with name '{}' and id '{}'", service.getName(), service.getId());
        return serviceBundleService.validate(service);
    }

    @GetMapping(path = "/by/{field}")
    public Map<String, List<OpenAIREService>> by(@PathVariable String field, @RequestParam("vocabularyType") String type) {
        logger.info("Requesting Services by [vocabulary={}]", type);
        return serviceBundleService.getByVocabulary("payload." + field, Vocabulary.Type.fromString(type));
    }
}
