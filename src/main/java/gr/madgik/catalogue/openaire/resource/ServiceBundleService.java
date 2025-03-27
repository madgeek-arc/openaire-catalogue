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

package gr.madgik.catalogue.openaire.resource;

import gr.madgik.catalogue.openaire.AbstractBundleService;
import gr.madgik.catalogue.openaire.Catalogue;
import gr.madgik.catalogue.openaire.domain.Bundle;
import gr.madgik.catalogue.openaire.domain.Service;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.domain.Vocabulary;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.madgik.catalogue.openaire.resource.repository.ServiceRepository;
import gr.madgik.catalogue.openaire.utils.BundleResourceOperations;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.openaire.vocabulary.service.VocabularyService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
public class ServiceBundleService extends AbstractBundleService<Service, ServiceBundle, String>
        implements BundleResourceOperations<Service, ServiceBundle, String> {

    private final Catalogue<ServiceBundle, String> catalogue;
    private final RegistryCoreRepository<ServiceBundle, String> repository;
    private final VocabularyService vocabularyService;
    private final ProviderResourcesCommonMethods commonMethods;

    public ServiceBundleService(Catalogue<ServiceBundle, String> catalogue,
                                ServiceRepository repository,
                                VocabularyService vocabularyService,
                                ProviderResourcesCommonMethods commonMethods) {
        super(catalogue, repository);
        this.catalogue = catalogue;
        this.repository = repository;
        this.vocabularyService = vocabularyService;
        this.commonMethods = commonMethods;
    }


    @Override
    public Service register(Service service) {
        return catalogue.register(new ServiceBundle(service)).getPayload();
    }

    @Override
    public boolean validate(Object resource) {
        return commonMethods.validate(resource);
    }

    @Override
    public ServiceBundle verify(String id, String status, Boolean active) {
        ServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        commonMethods.logVerificationAndActivation(bundle, status, null);
        return repository.update(id, bundle);
    }

    @Override
    public ServiceBundle activate(String id, Boolean active) {
        ServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        commonMethods.logVerificationAndActivation(bundle, null, active);
        return repository.update(id, bundle);
    }

    public Map<String, List<Service>> getByVocabulary(String field, String type) {
        Map<String, List<Service>> map = new HashMap<>();
        List<Vocabulary> vocabularies = vocabularyService.getByType(type);
        FacetFilter filter = new FacetFilter();
        for (Vocabulary vocabulary : vocabularies) {
            filter.addFilter(field, vocabulary.getId());
            map.put(vocabulary.getName(), get(filter).map(Bundle::getPayload).getResults());
        }
        return map;
    }
}
