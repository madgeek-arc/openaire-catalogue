package gr.madgik.catalogue.eosc.provider.repository;

import eu.einfracentral.domain.ProviderBundle;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import org.springframework.stereotype.Component;

@Component
public class ProviderRepository extends RegistryCoreRepository<ProviderBundle> {

    public ProviderRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "provider";
    }
}
