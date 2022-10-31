package gr.madgik.catalogue.openaire.resource.repository;

import eu.einfracentral.domain.ResourceBundle;
import eu.einfracentral.domain.Service;
import gr.madgik.catalogue.repository.Repository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ResourceRepository<T extends ResourceBundle<? extends Service>, ID> extends Repository<T, ID> {


    @Query(value = "{ $and: [{'payload.id' : ?0}, { 'payload.catalogueId' : ?1} ]}")
    T get(ID id, String catalogue);
}
