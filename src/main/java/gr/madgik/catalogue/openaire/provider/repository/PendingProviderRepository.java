package gr.madgik.catalogue.openaire.provider.repository;

import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.openaire.domain.ProviderBundle;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import org.springframework.stereotype.Component;

@Component
public class PendingProviderRepository extends RegistryCoreRepository<ProviderBundle, String> {

    public PendingProviderRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "pending_provider";
    }

}
