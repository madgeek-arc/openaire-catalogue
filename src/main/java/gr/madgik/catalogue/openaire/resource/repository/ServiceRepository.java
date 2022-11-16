package gr.madgik.catalogue.openaire.resource.repository;

import gr.madgik.catalogue.openaire.BundleMongoRepository;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends BundleMongoRepository<ServiceBundle, String> {

}
