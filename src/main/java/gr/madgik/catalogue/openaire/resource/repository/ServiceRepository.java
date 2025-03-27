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

package gr.madgik.catalogue.openaire.resource.repository;

import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Component;

@Component
public class ServiceRepository extends RegistryCoreRepository<ServiceBundle, String> implements ResourceRepository<ServiceBundle, String> {


    public ServiceRepository(GenericResourceService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "service";
    }

    @Override
    public ServiceBundle get(@NotNull String s, String catalogue) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("resource_internal_id", s);
        if (catalogue != null) {
            filter.addFilter("catalogue_id", catalogue);
        }
        Paging<ServiceBundle> results = get(filter);
        if (results.getResults() != null && !results.getResults().isEmpty()) {
            return results.getResults().get(0);
        }
        return null;
    }
}
