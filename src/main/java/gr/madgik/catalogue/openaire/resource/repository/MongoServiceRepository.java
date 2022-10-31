package gr.madgik.catalogue.openaire.resource.repository;

import eu.einfracentral.domain.*;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MongoServiceRepository extends MongoRepository<ServiceBundle, String>, ResourceRepository<ServiceBundle, String> {

    List<ResourceBundle<Service>> findByIdAndPayload_CatalogueId(String id, String catalogueId);

    @Query(value = "{ 'payload.resourceOrganization' : ?0 }", fields = "{ 'payload.resourceOrganization' : 1 }")
    List<ServiceBundle> findAllByPayload_ResourceOrganization(String providerId);

    default List<ServiceBundle> getServices(String providerId) {
        return findAllByPayload_ResourceOrganization(providerId);
    }

    @Override
    default ServiceBundle create(ServiceBundle resource) {
        return this.save(resource);
    }

    @Override
    default ServiceBundle get(String s) {
        return this.findById(s).orElse(null);
    }

    @Override
    default Paging<ServiceBundle> get(FacetFilter filter) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    default Page<ServiceBundle> get(Pageable filter) {
        return ResourceRepository.super.get(filter);
    }

    @Override
    default ServiceBundle update(String s, ServiceBundle resource) {
        return this.save(resource);
    }
}
