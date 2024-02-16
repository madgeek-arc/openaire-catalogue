package gr.madgik.catalogue.openaire.resource;

import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.openaire.AbstractBundleService;
import gr.madgik.catalogue.openaire.domain.Datasource;
import gr.madgik.catalogue.openaire.domain.DatasourceBundle;
import gr.madgik.catalogue.openaire.resource.repository.DatasourceRepository;

@org.springframework.stereotype.Service
public class DatasourceBundleService extends AbstractBundleService<Datasource, DatasourceBundle, String>
        implements BundleResourceOperations<Datasource, DatasourceBundle, String> {

    private final Catalogue<DatasourceBundle, String> catalogue;
    private final DatasourceRepository repository;

    public DatasourceBundleService(Catalogue<DatasourceBundle, String> catalogue,
                                   DatasourceRepository repository) {
        super(catalogue, repository);
        this.catalogue = catalogue;
        this.repository = repository;
    }

    @Override
    public boolean validate(Object resource) {
        throw new UnsupportedOperationException("Not implemented yet");
//        return null;
    }

    @Override
    public Datasource register(Datasource provider) {
        return catalogue.register(new DatasourceBundle(provider)).getDatasource();
    }

    @Override
    public DatasourceBundle verify(String id, String status, Boolean active) {
        DatasourceBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        return catalogue.update(id, bundle);
    }

    @Override
    public DatasourceBundle activate(String id, Boolean active) {
        DatasourceBundle bundle = repository.get(id);
        bundle.setActive(active);
        return catalogue.update(id, bundle);
    }
}
