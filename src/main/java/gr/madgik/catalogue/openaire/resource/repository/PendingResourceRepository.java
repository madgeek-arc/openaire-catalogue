package gr.madgik.catalogue.openaire.service.repository;

import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import org.springframework.stereotype.Component;

@Component
public class PendingResourceRepository extends RegistryCoreRepository<ServiceBundle, String> {

    public PendingResourceRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "pending_service";
    }
}
