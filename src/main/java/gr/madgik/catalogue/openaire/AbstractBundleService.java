package gr.madgik.catalogue.openaire;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.Identifiable;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import eu.openminted.registry.core.service.ServiceException;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;


public abstract class AbstractBundleService<T extends Identifiable, B extends Bundle<T>, ID extends String> implements BundleResourceOperations<T, B, ID> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBundleService.class);

    private final Catalogue<B, String> catalogue;
    private final RegistryCoreRepository<B, String> repository;

    protected AbstractBundleService(Catalogue<B, String> catalogue, RegistryCoreRepository<B, String> repository) {
        this.catalogue = catalogue;
        this.repository = repository;
    }

    @Override
    public boolean validate(Object resource) {
        return false;
    }

    @Override
    public B create(B resource) {
        return catalogue.register(resource);
    }

    @Override
    public B update(ID id, B resource) {
        return catalogue.update(id, resource);
    }

    @Override
    public void delete(ID id) {
        catalogue.delete(id);
    }

    @Override
    public T get(ID id) {
        return repository.get(id).getPayload();
    }

    @Override
    public B getBundle(ID id) {
        return repository.get(id);
    }

    @Override
    public Paging<B> get(FacetFilter filter) {
        return repository.get(filter);
    }

    @Override
    public List<T> getByIds(ID... ids) {
        List<T> resources;
        resources = Arrays.stream(ids)
                .map(id ->
                {
                    try {
                        return get(id);
                    } catch (ServiceException | ResourceNotFoundException e) {
                        return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(toList());
        return resources;
    }
}
