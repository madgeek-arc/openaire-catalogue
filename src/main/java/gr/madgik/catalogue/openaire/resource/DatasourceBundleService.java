package gr.madgik.catalogue.openaire.resource;

import eu.einfracentral.domain.Datasource;
import eu.einfracentral.domain.DatasourceBundle;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.openaire.AbstractBundleService;
import gr.madgik.catalogue.openaire.resource.repository.DatasourceRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.repository.Repository;
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
