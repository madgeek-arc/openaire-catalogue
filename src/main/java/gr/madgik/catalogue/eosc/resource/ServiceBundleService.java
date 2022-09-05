package gr.madgik.catalogue.eosc.resource;

import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.eosc.resource.repository.ServiceRepository;
import gr.madgik.catalogue.utils.PagingUtils;
import org.springframework.data.domain.Pageable;

import java.util.Objects;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ServiceBundleService implements BundleResourceOperations<Service, ServiceBundle, String> {

    private final Catalogue<ServiceBundle, String> catalogue;
    private final ServiceRepository repository;

    public ServiceBundleService(Catalogue<ServiceBundle, String> catalogue, ServiceRepository repository) {
        this.catalogue = catalogue;
        this.repository = repository;
    }

    @Override
    public boolean validate(Object resource) {
        throw new UnsupportedOperationException("Not implemented yet");
//        return null;
    }

    @Override
    public Service register(Service provider) {
        return catalogue.register(new ServiceBundle(provider)).getService();
    }

    @Override
    public Service update(String id, Service provider) {
        ServiceBundle bundle = repository.get(id);
        bundle.setService(provider);
        return catalogue.update(id, bundle).getService();
    }

    @Override
    public void delete(String id) {
        catalogue.delete(id);
    }

    @Override
    public Service get(String id) {
        ServiceBundle bundle = repository.get(id);
        return bundle != null ? bundle.getService() : null;
    }

    @Override
    public Paging<Service> get(Pageable pageable) {
        return repository.get(PagingUtils.toFacetFilter(pageable, repository.getResourceTypeName())).map(ServiceBundle::getService);
    }

    @Override
    public Paging<Service> get(FacetFilter filter) {
        Paging<ServiceBundle> bundlePaging = repository.get(filter);
        Paging<Service> services = new Paging<>(bundlePaging, bundlePaging.getResults().stream().filter(Objects::nonNull).map(ServiceBundle::getService).collect(Collectors.toList()));
        return services;
    }

    @Override
    public ServiceBundle verify(String id, String status, Boolean active) {
        ServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        return catalogue.update(id, bundle);
    }

    @Override
    public ServiceBundle activate(String id, Boolean active) {
        ServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        return catalogue.update(id, bundle);
    }
}
