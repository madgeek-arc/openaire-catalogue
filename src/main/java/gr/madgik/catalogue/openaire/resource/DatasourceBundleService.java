package gr.madgik.catalogue.openaire.resource;

import eu.einfracentral.domain.Datasource;
import eu.einfracentral.domain.DatasourceBundle;
import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.openaire.AbstractResourceBundleMongoService;
import gr.madgik.catalogue.openaire.resource.repository.DatasourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Map;

@org.springframework.stereotype.Service
public class DatasourceBundleService extends AbstractResourceBundleMongoService<Datasource, DatasourceBundle, String> implements BundleResourceOperations<Datasource, DatasourceBundle, String> {

    private final Catalogue<DatasourceBundle, String> catalogue;
    private final DatasourceRepository repository;

    public DatasourceBundleService(Catalogue<DatasourceBundle, String> catalogue,
                                   DatasourceRepository repository,
                                   MongoTemplate mongoTemplate) {
        super(DatasourceBundle.class, mongoTemplate);
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
    public DatasourceBundle create(DatasourceBundle resource) {
        return null;
    }

    @Override
    public Datasource update(String id, Datasource provider) {
        DatasourceBundle bundle = repository.get(id);
        bundle.setDatasource(provider);
        return catalogue.update(id, bundle).getDatasource();
    }

    @Override
    public void delete(String id) {
        catalogue.delete(id);
    }

    @Override
    public Datasource get(String id) {
        DatasourceBundle bundle = repository.get(id);
        return bundle != null ? bundle.getDatasource() : null;
    }

    @Override
    public Page<Datasource> get(Pageable pageable) {
//        return repository.get(PagingUtils.toFacetFilter(pageable, repository.getResourceTypeName())).map(DatasourceBundle::getDatasource);
        return null;
    }

    @Override
    public Page<Datasource> get(FacetFilter filter) {
//        Paging<DatasourceBundle> bundlePaging = repository.get(filter);
//        Paging<Datasource> services = new Paging<>(bundlePaging, bundlePaging.getResults().stream().filter(Objects::nonNull).map(DatasourceBundle::getDatasource).collect(Collectors.toList()));
//        return services;
        return null;
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
