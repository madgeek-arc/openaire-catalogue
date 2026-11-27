package eu.openaire.catalogue.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogueResourcesControllerTest {

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CatalogueResourcesController()).build();

    @Test
    void listingRedirectsToOaServicesWithTheQuery() throws Exception {
        mvc.perform(get("/api/catalogue-resources?from=0&quantity=100&order=asc&orderField=name").contextPath("/api"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        "http://localhost/api/oa/services?from=0&quantity=100&order=asc&orderField=name"));
    }

    @Test
    void listingWithoutAQueryRedirectsToOaServices() throws Exception {
        mvc.perform(get("/api/catalogue-resources").contextPath("/api"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost/api/oa/services"));
    }

    @Test
    void subRoutesAnswerNotFound() throws Exception {
        for (String path : new String[]{"/catalogue-resources/abc", "/catalogue-resources/abc/resourceType",
                "/catalogue-resources/by/category"}) {
            mvc.perform(get(path))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail", containsString("/oa/services")));
        }
    }
}
