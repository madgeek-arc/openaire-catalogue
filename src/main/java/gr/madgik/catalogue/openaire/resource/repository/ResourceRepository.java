package gr.madgik.catalogue.openaire.resource.repository;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.Identifiable;
import gr.madgik.catalogue.repository.Repository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ResourceRepository<T extends Identifiable, B extends Bundle<T>, ID> extends Repository<B, ID> {


    @Query(value = "{ $and: [{'payload.id' : ?0}, { 'payload.catalogueId' : ?1} ]}")
    B get(ID id, String catalogue);
}
