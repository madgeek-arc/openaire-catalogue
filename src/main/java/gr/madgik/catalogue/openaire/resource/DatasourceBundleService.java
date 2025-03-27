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
import gr.madgik.catalogue.openaire.domain.Datasource;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.repository.Repository;
import gr.madgik.catalogue.openaire.resource.repository.DatasourceRepository;
import gr.madgik.catalogue.openaire.utils.BundleResourceOperations;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import org.springframework.stereotype.Service;

@Service
public class DatasourceBundleService extends AbstractBundleService<Datasource, DatasourceBundle, String>
        implements BundleResourceOperations<Datasource, DatasourceBundle, String> {

    private final Catalogue<DatasourceBundle, String> catalogue;
    private final Repository<DatasourceBundle, String> repository;
    private final ProviderResourcesCommonMethods commonMethods;

    public DatasourceBundleService(Catalogue<DatasourceBundle, String> catalogue,
                                   DatasourceRepository repository,
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
    public Datasource register(Datasource datasource) {
        return catalogue.register(new DatasourceBundle(datasource)).getDatasource();
    }

    @Override
    public DatasourceBundle verify(String id, String status, Boolean active) {
        DatasourceBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        commonMethods.logVerificationAndActivation(bundle, status, null);
        return repository.update(id, bundle);
    }

    @Override
    public DatasourceBundle activate(String id, Boolean active) {
        DatasourceBundle bundle = repository.get(id);
        bundle.setActive(active);
        commonMethods.logVerificationAndActivation(bundle, null, active);
        return repository.update(id, bundle);
    }
}
