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

package gr.madgik.catalogue.openaire.resource.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.madgik.catalogue.openaire.FacetLabelService;
import gr.madgik.catalogue.openaire.domain.Bundle;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.domain.Resource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static gr.madgik.catalogue.openaire.vocabulary.service.VocabularyService.logger;

@RestController
@RequestMapping("catalogue-resources")
public class CatalogueResourcesController {

    private final GenericResourceService genericResourceService;
    private final FacetLabelService facetLabelService;
    private final ObjectMapper objectMapper;


    public CatalogueResourcesController(GenericResourceService genericResourceService,
                                        FacetLabelService facetLabelService) {
        this.genericResourceService = genericResourceService;
        this.facetLabelService = facetLabelService;
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @GetMapping("{id}")
    public <T extends Bundle<?>> Object get(@PathVariable("id") String id) {
        T bundle = genericResourceService.get("resources", id);
        return bundle.getPayload();
    }

    @GetMapping("{id}/resourceType")
    public Map.Entry<String, String> getResourceType(@PathVariable("id") String id) {
        Resource resource = genericResourceService.searchResource("resources", id, true);
        return new AbstractMap.SimpleEntry<>("resourceType", resource.getResourceTypeName());
    }

    @BrowseParameters
    @Operation(summary = "Browse Catalogue Resources.")
    @GetMapping
    public Paging<?> getCatalogueResources(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType("resources");
        Paging<?> paging = genericResourceService.getResults(filter).map(r -> ((Bundle<?>) r).getPayload());
        paging.setFacets(facetLabelService.createLabels(paging.getFacets()));
        return paging;
    }

    @GetMapping("bundles/{id}")
    public <T extends Bundle<?>> Object getBundle(@PathVariable("id") String id) {
        return genericResourceService.get("resources", id);
    }

    @BrowseParameters
    @Operation(summary = "Browse Catalogue Resource Bundles.")
    @GetMapping("bundles")
    public Paging<?> getCatalogueResourceBundles(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setResourceType("resources");
        Paging<?> paging = genericResourceService.getResults(filter);
        paging.setFacets(facetLabelService.createLabels(paging.getFacets()));
        return paging;
    }

    @Operation(summary = "Get all Resources in the catalogue organized by an attribute, e.g. get Resources organized in categories.")
    @GetMapping("by/{field}")
    public <T extends Bundle<? extends gr.madgik.catalogue.openaire.domain.Service>> Map<String, List<?>> getBy(@PathVariable(value = "field") String field,
                                                                                                                @RequestParam MultiValueMap<String, Object> allRequestParams) {
        Map<String, List<T>> results;
        FacetFilter filter = FacetFilter.from(allRequestParams);
        filter.setQuantity(10_000);
        filter.setResourceType("resources");
        results = genericResourceService.getResultsGrouped(filter, field);
        Map<String, List<?>> resources = new TreeMap<>();
        results.forEach((key, value) ->
                resources.put(getResourceName(key), value
                        .stream()
                        .map(Bundle::getPayload)
                        .sorted(Comparator.comparing(gr.madgik.catalogue.openaire.domain.Service::getName))
                        .toList()
                )
        );
        return resources;
    }

    private String getResourceName(String key) {
        String name = key;
        try {
            Object result = genericResourceService.get("resourceTypes", key);
            IdName idName = objectMapper.convertValue(result, IdName.class);
            name = idName.getName();
        } catch (Exception e) {
            logger.warn("Could not find resource name. Using id instead.", e);
        }
        return name;
    }

    private static class IdName {
        String id;
        String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
