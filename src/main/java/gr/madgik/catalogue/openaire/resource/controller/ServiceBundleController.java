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

import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.dto.BulkOperation;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.madgik.catalogue.openaire.resource.ServiceBundleService;
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
@RequestMapping("/bundles/services")
public class ServiceBundleController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBundleController.class);

    private final ServiceBundleService serviceBundleService;
    private final RegistryCoreRepository<ServiceBundle, String> serviceRepository;

    public ServiceBundleController(ServiceBundleService serviceBundleService,
                                   RegistryCoreRepository<ServiceBundle, String> serviceRepository) {
        super();
        this.serviceBundleService = serviceBundleService;
        this.serviceRepository = serviceRepository;
    }


    @BrowseParameters
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public Paging<ServiceBundle> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> allRequestParams) {
        return serviceBundleService.getWithEnrichedFacets(FacetFilter.from(allRequestParams));
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM') or isServiceProviderAdmin(#id)")
    public ServiceBundle get(@PathVariable("id") String id) {
        return serviceRepository.findById(id).orElse(null);
    }

    @PatchMapping(path = "{id}/verify", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ServiceBundle verify(@PathVariable("id") String id, @RequestParam(required = false) Boolean active,
                                @RequestParam(required = false) String status) {
        ServiceBundle service = serviceBundleService.verify(id, status, active);
        logger.info("User updated Service with name '{}' [status: {}] [active: {}]", service.getPayload().getName(), status, active);
        return service;
    }

    // Activate/Deactivate a Service.
    @PatchMapping(path = "{id}/publish", produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public ServiceBundle publish(@PathVariable("id") String id, @RequestParam(required = false) Boolean active) {
        ServiceBundle service = serviceBundleService.activate(id, active);
        logger.info("User updated Service with name '{}' [active: {}]", service.getPayload().getName(), active);
        return service;
    }

    @PostMapping("bulk")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public BulkOperation<ServiceBundle> addAll(@RequestBody List<ServiceBundle> bundles) {
        BulkOperation<ServiceBundle> services = new BulkOperation<>();
        for (ServiceBundle bundle : bundles) {
            try {
                services.getSuccessful().add(serviceRepository.create(bundle));
            } catch (Exception e) {
                services.getFailed().add(bundle);
            }
        }
        return services;
    }

    @PutMapping("bulk")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ONBOARDING_TEAM')")
    public BulkOperation<ServiceBundle> updateAll(@RequestBody List<ServiceBundle> bundles) {
        BulkOperation<ServiceBundle> services = new BulkOperation<>();
        for (ServiceBundle bundle : bundles) {
            try {
                services.getSuccessful().add(serviceRepository.update(bundle.getId(), bundle));
            } catch (Exception e) {
                services.getFailed().add(bundle);
            }
        }
        return services;
    }
}
