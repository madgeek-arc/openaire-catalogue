package gr.madgik.catalogue.openaire.resource.repository;

import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import org.springframework.stereotype.Component;

@Component
public class PendingResourceRepository extends RegistryCoreRepository<ServiceBundle, String> {

    public PendingResourceRepository(GenericResourceService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "pending_service";
    }
}
