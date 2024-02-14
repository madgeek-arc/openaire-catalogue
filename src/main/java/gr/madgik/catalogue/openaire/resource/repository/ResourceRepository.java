package gr.madgik.catalogue.openaire.resource.repository;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.Identifiable;
import gr.madgik.catalogue.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface ResourceRepository<B extends Bundle<? extends Identifiable>, ID> extends Repository<B, ID> {

    B get(ID id, String catalogue);
}
