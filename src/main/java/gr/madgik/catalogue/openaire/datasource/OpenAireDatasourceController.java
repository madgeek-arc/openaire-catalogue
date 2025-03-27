/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.madgik.catalogue.openaire.datasource;

import com.fasterxml.jackson.core.JsonProcessingException;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import io.swagger.v3.oas.annotations.Parameter;
import org.json.simple.parser.ParseException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class OpenAireDatasourceController {

    private final OpenAireDatasourceService openAireDatasourceService;

    public OpenAireDatasourceController(OpenAireDatasourceService openAireDatasourceService) {
        this.openAireDatasourceService = openAireDatasourceService;
    }


    @BrowseParameters
    @GetMapping("dsm/datasources")
    public Paging<?> get(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) throws ParseException, JsonProcessingException {
        return this.openAireDatasourceService.getOpenAIREDatasourcesAsJSON(FacetFilter.from(allRequestParams));
    }

    @GetMapping("provide/datasources/{id}")
    public Object getByID(@PathVariable("id") String id) throws ParseException {
        return this.openAireDatasourceService.getEnrichedOpenAIREDatasourceById(id);
    }
}
