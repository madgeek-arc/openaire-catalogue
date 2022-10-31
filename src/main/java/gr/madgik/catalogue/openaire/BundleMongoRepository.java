package gr.madgik.catalogue.openaire;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.ProviderBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.athenarc.catalogue.exception.ResourceAlreadyExistsException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BundleMongoRepository<T extends Bundle<?>, ID> extends MongoRepository<T, ID>, gr.madgik.catalogue.repository.Repository<T, ID> {

    Page<T> findAll(Pageable pageable);

    List<ProviderBundle> findByPayload_IdAndPayload_CatalogueId(String id, String catalogueId);

    @Override
    @Query(value = "{ 'payload.id' : ?0 }")
    Optional<T> findById(ID id);

    default T get(ID id) {
        return findById(id).orElse(null);
    }

    @Override
    default Page<T> get(Pageable pageable) {
        return findAll(pageable);
    }

    @Override
    default T create(T resource) {
        T existing = get((ID) resource.getId());
        if (existing != null) {
            throw new ResourceAlreadyExistsException();
        }
        return save(resource);
    }

    @Override
    default T update(ID s, T resource) {
        return save(resource);
    }

    @Override
    default Paging<T> get(FacetFilter filter) {
        throw new UnsupportedOperationException();
    }
}
