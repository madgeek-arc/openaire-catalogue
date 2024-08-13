package gr.madgik.catalogue.openaire;

import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.utils.PagingUtils;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

public abstract class AbstractResourceController<T> {

    protected final ResourceOperations<T, String> service;

    private static final Logger logger = LoggerFactory.getLogger(AbstractResourceController.class);

    AbstractResourceController(ResourceOperations<T, String> service) {
        this.service = service;
    }

    @GetMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<T> get(@PathVariable("id") String id) {
        return new ResponseEntity<>(service.get(id), HttpStatus.OK);
    }

    @PostMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<T> add(@RequestBody T t) {
        return new ResponseEntity<>(service.create(t), HttpStatus.CREATED);
    }

    @PutMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<T> update(@PathVariable("id") String id, @RequestBody T t) throws ResourceNotFoundException {
        return new ResponseEntity<>(service.update(id, t), HttpStatus.OK);
    }

    @PostMapping(path = "validate", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Boolean> validate(@RequestBody T t) {
        return new ResponseEntity<>(service.validate(t), HttpStatus.OK);
    }

    @DeleteMapping(path = "{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        service.delete(id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Filter a list of Resources based on a set of filters.
    @Browse
    @GetMapping(path = "all", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Paging<T>> getAll(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) {
        FacetFilter ff = PagingUtils.createFacetFilter(allRequestParams);
        return new ResponseEntity<>(service.get(ff), HttpStatus.OK);
    }

    @GetMapping(path = "byId", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<T>> getSome(@RequestParam String[] ids) {
        return new ResponseEntity<>(service.get(ids), HttpStatus.OK);
    }

//        @GetMapping(path = "by/{field}", produces = {MediaType.APPLICATION_JSON_VALUE})
//        public ResponseEntity<Map<String, List<T>>> getBy(@PathVariable String field) {
//            return new ResponseEntity<>(service.getBy(field), HttpStatus.OK);
//        }
}
