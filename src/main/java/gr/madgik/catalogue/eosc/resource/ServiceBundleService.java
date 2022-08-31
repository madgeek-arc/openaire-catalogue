package gr.madgik.catalogue.eosc.resource;

import eu.einfracentral.domain.Service;
import eu.einfracentral.domain.ServiceBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.eosc.resource.repository.MongoServiceRepository;
import gr.madgik.catalogue.eosc.resource.repository.ServiceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Objects;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
public class ServiceBundleService {

    private final Catalogue<ServiceBundle, String> catalogue;
    private final ServiceRepository repository;

    public ServiceBundleService(Catalogue<ServiceBundle, String> catalogue, ServiceRepository repository) {
        this.catalogue = catalogue;
        this.repository = repository;
    }

    public boolean validate(Service provider) {
        throw new UnsupportedOperationException("Not implemented yet");
//        return null;
    }

    public Service register(Service provider) {
        return catalogue.register(new ServiceBundle(provider)).getService();
    }

    public Service update(String id, Service provider) {
        ServiceBundle bundle = repository.get(id);
        bundle.setService(provider);
        return catalogue.update(id, bundle).getService();
    }

    public void delete(String id) {
        catalogue.delete(id);
    }

    public Service get(String id) {
        ServiceBundle bundle = repository.get(id);
        return bundle != null ? bundle.getService() : null;
    }

    public Page<Service> get(Pageable pageable) {
        return repository.get(pageable).map(ServiceBundle::getService);
    }

    public Paging<Service> get(FacetFilter filter) {
        Paging<ServiceBundle> bundlePaging = repository.get(filter);
        Paging<Service> services = new Paging<>(bundlePaging, bundlePaging.getResults().stream().filter(Objects::nonNull).map(ServiceBundle::getService).collect(Collectors.toList()));
        return services;
    }

    public ServiceBundle verify(String id, String status, Boolean active) {
        ServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        return catalogue.update(id, bundle);
    }

    public ServiceBundle activate(String id, Boolean active) {
        ServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        return catalogue.update(id, bundle);
    }
}
