package gr.madgik.catalogue.openaire;

import gr.madgik.catalogue.openaire.dto.BulkOperation;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractResourceOperations<T extends Identifiable> implements ResourceOperations<T, String> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResourceOperations.class);
    protected final RegistryCoreRepository<T, String> registryCoreRepository;

    public AbstractResourceOperations(RegistryCoreRepository<T, String> registryCoreRepository) {
        this.registryCoreRepository = registryCoreRepository;
    }

    @Override
    public List<T> get(String... ids) {
        List<T> resources = new ArrayList<>();
        for (String id : ids) {
            resources.add(get(id));
        }
        return resources;
    }

    @Override
    public T create(T resource) {
        this.validate(resource);
        return registryCoreRepository.create(resource);
    }

    @Override
    public T update(String s, T resource) {
        this.validate(resource);
        return registryCoreRepository.update(s, resource);
    }

    @Override
    public void delete(String s) {
        registryCoreRepository.deleteById(s);
    }

    @Override
    public T get(String s) {
        return registryCoreRepository.get(s);
    }

    @Override
    public Paging<T> get(FacetFilter filter) {
        return registryCoreRepository.get(filter);
    }

    @Override
    public BulkOperation<T> bulkAdd(List<T> resources) {
        BulkOperation<T> items = new BulkOperation<>();
        for (T res : resources) {
            try {
                items.getSuccessful().add(this.create(res));
            } catch (Exception e) {
                items.getFailed().add(res);
            }
        }
        return items;
    }

    @Override
    public BulkOperation<T> bulkUpdate(List<T> resources) {
        BulkOperation<T> items = new BulkOperation<>();
        for (T res : resources) {
            try {
                items.getSuccessful().add(this.update(res.getId(), res));
            } catch (Exception e) {
                items.getFailed().add(res);
            }
        }
        return items;
    }
}
