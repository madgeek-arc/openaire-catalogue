package gr.madgik.catalogue.openaire.resource.repository;

import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Identifiable;
import gr.madgik.catalogue.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ResourceRepository<B extends Bundle<? extends Identifiable>, ID> extends Repository<B, ID> {

    B get(ID id, String catalogue);
}
