package gr.madgik.catalogue.openaire.datasource;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.catalogue.annotations.Browse;
import gr.athenarc.catalogue.utils.PagingUtils;
import io.swagger.v3.oas.annotations.Parameter;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class OpenAireDatasourceController {

    private final OpenAireDatasourceService openAireDatasourceService;

    public OpenAireDatasourceController(OpenAireDatasourceService openAireDatasourceService) {
        this.openAireDatasourceService = openAireDatasourceService;
    }


    @Browse
    @GetMapping("dsm/datasources")
    public Paging<?> get(@Parameter(hidden = true) @RequestParam Map<String, Object> allRequestParams) throws ParseException, JsonProcessingException {
        return this.openAireDatasourceService.getOpenAIREDatasourcesAsJSON(PagingUtils.createFacetFilter(allRequestParams));
    }

    @GetMapping("provide/datasources/{id}")
    public Object getByID(@PathVariable("id") String id) throws ParseException {
        return this.openAireDatasourceService.getEnrichedOpenAIREDatasourceById(id);
    }
}
