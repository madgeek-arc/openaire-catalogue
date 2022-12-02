package gr.madgik.catalogue.openaire.datasource;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.madgik.catalogue.openaire.domain.Service;
import gr.madgik.catalogue.utils.PagingUtils;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Map;

@RestController
public class OpenAireDatasourceController {

    private final OpenAireDatasourceService openAireDatasourceService;

    public OpenAireDatasourceController(OpenAireDatasourceService openAireDatasourceService) {
        this.openAireDatasourceService = openAireDatasourceService;
    }


    @ApiImplicitParams({
            @ApiImplicitParam(name = "query", value = "Keyword to refine the search", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "from", value = "Starting index in the result set", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "quantity", value = "Quantity to be fetched", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "order", value = "asc / desc", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "orderField", value = "Order field", dataType = "string", paramType = "query")
    })
    @GetMapping("dsm/datasources")
    public Paging<?> get(@ApiIgnore @RequestParam Map<String, Object> allRequestParams) throws ParseException, JsonProcessingException {
        return this.openAireDatasourceService.getOpenAIREDatasourcesAsJSON(PagingUtils.createFacetFilter(allRequestParams));
    }

    @GetMapping("provide/datasources/{id}")
    public Object getByID(@PathVariable("id") String id) throws ParseException {
        return this.openAireDatasourceService.getEnrichedOpenAIREDatasourceById(id);
    }
}
