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

import gr.madgik.catalogue.openaire.domain.Bundle;
import gr.madgik.catalogue.openaire.domain.Service;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.resource.ServiceBundleService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

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
    public Service get(@PathVariable("id") String id) {
        return serviceBundleService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isProviderAdmin(#service.resourceOrganisation)")
    public Service add(@RequestBody Service service) {
        return serviceBundleService.register(service); // TODO: change this ??
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isServiceProviderAdmin(#service)")
    public Service update(@PathVariable String id, @RequestBody Service service) {
        return serviceBundleService.update(id, new ServiceBundle(service)).getService(); // TODO: change this ??
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isServiceProviderAdmin(#id)")
    public void delete(@PathVariable String id) {
        serviceBundleService.delete(id);
    }

    @BrowseParameters
    @GetMapping
    public Paging<Service> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        return serviceBundleService.getWithEnrichedFacets(FacetFilter.from(allRequestParams)).map(Bundle::getPayload);
    }

    @PostMapping("validate")
    public boolean validate(@RequestBody Service service) {
        logger.info("Validating Service with name '{}' and id '{}'", service.getName(), service.getId());
        return serviceBundleService.validate(service);
    }

    @GetMapping("/by/{field}")
    public Map<String, List<Service>> by(@PathVariable String field, @RequestParam("vocabularyType") String type) {
        logger.info("Requesting Services by [vocabulary={}]", type);
        return serviceBundleService.getByVocabulary(field, type);
    }

    @Parameters({
            @Parameter(
                    name = "ids",
                    description = "Comma-separated list of Resource ids",
                    in = ParameterIn.PATH,
                    required = true,
                    schema = @Schema(type = "string")
            )
    })
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EPOT')")
    @GetMapping(path = "ids/{ids}", produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<List<Service>> getSomeServices(@PathVariable("ids") String[] ids, @Parameter(hidden = true) Authentication auth) {
        return ResponseEntity.ok(serviceBundleService.getByIds(ids));
    }
}
