package gr.madgik.catalogue.openaire.provider.repository;

import eu.einfracentral.domain.ProviderBundle;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends MongoRepository<ProviderBundle, String>, gr.madgik.catalogue.repository.Repository<ProviderBundle, String> {

    Page<ProviderBundle> findAll(Pageable pageable);

    List<ProviderBundle> findByPayload_IdAndPayload_CatalogueId(String id, String catalogueId);

    @Override
    @Query(value = "{ 'payload.id' : ?0 }")
    Optional<ProviderBundle> findById(String id);

    default ProviderBundle get(String id) {
        return findById(id).orElse(null);
    }

    @Override
    default Page<ProviderBundle> get(Pageable pageable) {
        return findAll(pageable);
    }

    @Override
    default ProviderBundle create(ProviderBundle resource) {
        return save(resource);
    }

    @Override
    default ProviderBundle update(String s, ProviderBundle resource) {
        return save(resource);
    }

    @Override
    default Paging<ProviderBundle> get(FacetFilter filter) {
        throw new UnsupportedOperationException();
    }
}
