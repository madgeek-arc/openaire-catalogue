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

package gr.madgik.catalogue.openaire.provider;

import gr.madgik.catalogue.openaire.AbstractBundleService;
import gr.madgik.catalogue.openaire.Catalogue;
import gr.madgik.catalogue.openaire.domain.Provider;
import gr.madgik.catalogue.openaire.domain.ProviderBundle;
import gr.madgik.catalogue.openaire.provider.repository.ProviderRepository;
import gr.madgik.catalogue.openaire.repository.Repository;
import gr.madgik.catalogue.openaire.utils.BundleResourceOperations;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import org.springframework.stereotype.Service;

@Service
public class ProviderService extends AbstractBundleService<Provider, ProviderBundle, String>
        implements BundleResourceOperations<Provider, ProviderBundle, String> {

    private final Catalogue<ProviderBundle, String> catalogue;
    private final Repository<ProviderBundle, String> repository;
    private final ProviderResourcesCommonMethods commonMethods;

    public ProviderService(Catalogue<ProviderBundle, String> catalogue,
                           ProviderRepository repository,
                           ProviderResourcesCommonMethods commonMethods) {
        super(catalogue, repository);
        this.catalogue = catalogue;
        this.repository = repository;
        this.commonMethods = commonMethods;
    }

    @Override
    public boolean validate(Object resource) {
        return commonMethods.validate(resource);
    }

    @Override
    public Provider register(Provider provider) {
        return catalogue.register(new ProviderBundle(provider)).getProvider();
    }

    @Override
    public ProviderBundle verify(String id, String status, Boolean active) {
        ProviderBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        commonMethods.logVerificationAndActivation(bundle, status, null);
        return repository.update(id, bundle);
    }

    @Override
    public ProviderBundle activate(String id, Boolean active) {
        ProviderBundle bundle = repository.get(id);
        bundle.setActive(active);
        commonMethods.logVerificationAndActivation(bundle, null, active);
        return repository.update(id, bundle);
    }
}
