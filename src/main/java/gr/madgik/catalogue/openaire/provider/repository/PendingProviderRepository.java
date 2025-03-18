package gr.madgik.catalogue.openaire.provider.repository;

import gr.madgik.catalogue.openaire.domain.ProviderBundle;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import org.springframework.stereotype.Component;

@Component
public class PendingProviderRepository extends RegistryCoreRepository<ProviderBundle, String> {

    public PendingProviderRepository(GenericResourceService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "pending_provider";
    }

}
