package gr.madgik.catalogue.openaire.provider.repository;

import eu.einfracentral.domain.ProviderBundle;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import gr.madgik.catalogue.repository.Repository;
import org.springframework.stereotype.Component;

@Component
public class RegistryProviderRepository extends RegistryCoreRepository<ProviderBundle, String> implements Repository<ProviderBundle, String> {

    public RegistryProviderRepository(GenericItemService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "provider";
    }
}
