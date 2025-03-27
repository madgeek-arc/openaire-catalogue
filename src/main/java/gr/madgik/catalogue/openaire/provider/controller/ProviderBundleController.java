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

package gr.madgik.catalogue.openaire.provider.controller;

import gr.madgik.catalogue.openaire.domain.ProviderBundle;
import gr.madgik.catalogue.openaire.provider.ProviderService;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bundles/providers")
public class ProviderBundleController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderBundleController.class);

    private final ProviderService providerService;

    public ProviderBundleController(ProviderService providerService) {
        this.providerService = providerService;
    }


    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ProviderBundle verify(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                 @RequestParam(required = false) String status) {
        ProviderBundle provider = providerService.verify(id, status, active);
        logger.info("User updated Provider with name '{}' [status: {}] [active: {}]", provider.getProvider().getName(), status, active);
        return provider;
    }

    // Activate/Deactivate a Provider.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ProviderBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        ProviderBundle provider = providerService.activate(id, active);
        logger.info("User updated Provider with name '{}' [active: {}]", provider.getProvider().getName(), active);
        return provider;
    }

    @PostMapping("bulk")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public void addAll(@RequestBody List<ProviderBundle> providers) {
        for (ProviderBundle providerBundle : providers) {
            providerService.create(providerBundle); // TODO: change this ??
        }
//        return
    }

    @BrowseParameters
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public Paging<ProviderBundle> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        return providerService.getWithEnrichedFacets(FacetFilter.from(allRequestParams));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isProviderAdmin(#id)")
    public ProviderBundle get(@PathVariable("id") String id) {
        return providerService.getBundle(id);
    }
}
