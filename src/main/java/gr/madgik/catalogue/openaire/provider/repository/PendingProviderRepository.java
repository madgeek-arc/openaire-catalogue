package gr.madgik.catalogue.openaire.provider.repository;

import eu.einfracentral.domain.ProviderBundle;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
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
