package eu.openaire.catalogue.dsm;

import eu.openaire.catalogue.config.OpenAIREProperties;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Proxies the OpenAIRE datasource APIs: the registered datasources from the DSM API, and one enriched datasource from
 * the PROVIDE API.
 */
@RestController
@Tag(name = "OpenAIRE datasources", description = "Datasources registered in OpenAIRE")
public class DsmDatasourceController {

    private static final Set<String> RESERVED = Set.of("from", "quantity", "sort", "order", "keyword");

    private static final int MAX_CONNECTIONS_PER_HOST = 20;

    private final OpenAIREProperties properties;
    private final RestClient client;

    public DsmDatasourceController(OpenAIREProperties properties) {
        this.properties = properties;
        this.client = RestClient.builder()
                .requestFactory(requestFactory(properties.getDsm().getConnectTimeout(),
                        properties.getDsm().getReadTimeout()))
                .build();
    }

    @Operation(summary = "Returns the datasources registered in OpenAIRE. Any parameter other than from, quantity and "
            + "order is passed to the DSM API as a search field.")
    @GetMapping(path = "dsm/datasources", produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Object> search(@Parameter(hidden = true) @RequestParam Map<String, String> params) {
        int from = intParam(params, "from", 0);
        int quantity = Math.max(1, intParam(params, "quantity", 10));
        if (from < 0) {
            throw new ResourceException("'from' must not be negative", HttpStatus.BAD_REQUEST);
        }
        if (from % quantity != 0) {
            throw new ResourceException("'from' must be a multiple of 'quantity'", HttpStatus.BAD_REQUEST);
        }
        String order = String.valueOf(params.getOrDefault("order", "asc")).toLowerCase().startsWith("desc")
                ? "DESCENDING" : "ASCENDING";
        Map<String, String> fields = new LinkedHashMap<>(params);
        fields.keySet().removeAll(RESERVED);

        String uri = UriComponentsBuilder.fromUriString(base(properties.getDsm().getUrl()))
                .path("ds/searchregistered/{page}/{size}/")
                .queryParam("requestSortBy", "officialname")
                .queryParam("order", order)
                .buildAndExpand(from / quantity, quantity)
                .encode()
                .toUriString();
        Map<?, ?> response = call(() -> client.post().uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(fields)
                .retrieve()
                .body(Map.class));
        return paging(response);
    }

    @Operation(summary = "Returns the OpenAIRE datasource with the given id, enriched by the PROVIDE API.")
    @GetMapping(path = "provide/datasources/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object get(@PathVariable String id) {
        String uri = UriComponentsBuilder.fromUriString(base(properties.getDsm().getProvideUrl()))
                .path("repositories/getRepositoryById/{id}")
                .buildAndExpand(id)
                .encode()
                .toUriString();
        return call(() -> client.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve().body(Object.class));
    }

    /**
     * @return the value as an integer, or the fallback if it is missing or not a number
     */
    static int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString().trim()) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static Paging<Object> paging(Map<?, ?> response) {
        Paging<Object> paging = new Paging<>();
        if (response == null || !(response.get("header") instanceof Map<?, ?> header)) {
            paging.setResults(List.of());
            return paging;
        }
        List<Object> results = response.get("datasourceInfo") instanceof List<?> list ? List.copyOf(list) : List.of();
        int page = number(header.get("page"), 0);
        int size = number(header.get("size"), results.size());
        int total = number(header.get("total"), results.size());
        paging.setFrom(page * size);
        paging.setTo(Math.max(Math.min(paging.getFrom() + size, total) - 1, paging.getFrom() - 1));
        paging.setTotal(total);
        paging.setResults(results);
        return paging;
    }

    /**
     * The connect timeout also limits the wait for a free pooled connection. Requests are not retried: a retry waits
     * as long as the {@code Retry-After} header of the response says, whatever the timeouts.
     */
    private static HttpComponentsClientHttpRequestFactory requestFactory(Duration connectTimeout,
                                                                         Duration readTimeout) {
        CloseableHttpClient httpClient = HttpClients.custom()
                .useSystemProperties()
                .disableAutomaticRetries()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .useSystemProperties()
                        .setMaxConnPerRoute(MAX_CONNECTIONS_PER_HOST)
                        .setMaxConnTotal(2 * MAX_CONNECTIONS_PER_HOST)
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.of(connectTimeout))
                                .build())
                        .build())
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectionRequestTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }

    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException | HttpMessageConversionException e) {
            ResourceException failure = new ResourceException("The OpenAIRE datasource API failed",
                    HttpStatus.BAD_GATEWAY);
            failure.initCause(e);
            throw failure;
        }
    }

    private static String base(String url) {
        if (url == null || url.isBlank()) {
            throw new ResourceException("The OpenAIRE datasource API is not configured", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return url.endsWith("/") ? url : url + "/";
    }

    private static int intParam(Map<String, String> params, String name, int fallback) {
        try {
            return params.containsKey(name) ? Integer.parseInt(params.get(name)) : fallback;
        } catch (NumberFormatException e) {
            throw new ResourceException("'" + name + "' must be a number", HttpStatus.BAD_REQUEST);
        }
    }
}
