package gr.madgik.catalogue.openaire.datasource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
public class OpenAireDatasourceService {

    RestTemplate restTemplate = new RestTemplate();
    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openaire.dsm-api.url}")
    String dsmAPI;

    @Value("${openaire.provide.url}")
    String provideAPI;

    public Object getEnrichedOpenAIREDatasourceById(String id) throws ParseException {
        String response = createHttpRequest(searchEnrichedDatasourceByID(id).toUri(), "", HttpMethod.GET);
        Object result = new JSONObject();
        if (response != null) {
            result = new JSONParser().parse(response);
        }
        return result;
    }

    public Paging<Object> getOpenAIREDatasourcesAsJSON(FacetFilter ff) throws ParseException, JsonProcessingException {
        String page = createPage(ff);
        String size = Integer.toString(ff.getQuantity());
        String ordering = createOrdering(ff);
        String data = objectMapper.writeValueAsString(ff.getFilter());

        String response = createHttpRequest(searchDatasourceUri(page, size, ordering).toUri(), data, HttpMethod.POST);
        Paging<Object> paging = new Paging<>();
        if (response != null) {
            JSONObject obj = (JSONObject) new JSONParser().parse(response);
            paging = pagingOf((JSONObject) obj.remove("header"), (List<Object>) obj.get("datasourceInfo"));
        }
        return paging;
    }

    public static <K> Paging<K> pagingOf(JSONObject header, List<K> results) {
        Paging<K> paging = new Paging<>();
        int page = Integer.parseInt(header.get("page").toString());
        int size = Integer.parseInt(header.get("size").toString());
        int total = Integer.parseInt(header.get("total").toString());
        paging.setFrom(page * size);
        paging.setTo(paging.getFrom() + size - 1);
        if (paging.getTo() > total) {
            paging.setTo(total - 1);
        }
        paging.setTotal(total);
        paging.setResults(results);
        return paging;
    }

    private UriComponents searchEnrichedDatasourceByID(String id) {

        return UriComponentsBuilder
                .fromHttpUrl(provideAPI + "/repositories/getRepositoryById/")
                .path("/{id}")
                .build().expand(id).encode();
    }

    private UriComponents searchDatasourceUri(String page, String size, String order) {

        return UriComponentsBuilder
                .fromHttpUrl(dsmAPI + "/ds/searchregistered/")
                .path("/{page}/{size}/")
                .queryParam("requestSortBy", "officialname")
                .queryParam("order", order)
                .build().expand(page, size).encode();
    }

    private String createPage(FacetFilter ff) {
        int quantity = ff.getQuantity();
        if (ff.getFrom() % quantity != 0) {
            throw new RuntimeException("Unsupported paging parameters..");
        }
        return Integer.toString(ff.getFrom() / quantity);
    }

    private String createOrdering(FacetFilter ff) {
        String ordering = "ASCENDING";
        if (ff.getOrderBy() != null) {
            String order = ff.getOrderBy().get(ff.getOrderBy().keySet().toArray()[0]).toString();
            if (order.contains("desc")) {
                ordering = "DESCENDING";
            }
        }
        return ordering;
    }

    public String createHttpRequest(URI uri, String data, HttpMethod method) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("accept", "application/json");
        headers.add("Content-Type", "application/json");
        HttpEntity<Object> entity = new HttpEntity<>(data, headers);
        return restTemplate.exchange(uri.toString(), method, entity, String.class).getBody();
    }

}
