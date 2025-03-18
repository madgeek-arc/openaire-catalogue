package gr.madgik.catalogue.openaire.provider.repository;

import gr.madgik.catalogue.openaire.domain.ProviderBundle;
import gr.madgik.catalogue.openaire.repository.RegistryCoreRepository;
import gr.madgik.catalogue.openaire.repository.Repository;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import org.springframework.stereotype.Component;

@Component
public class ProviderRepository extends RegistryCoreRepository<ProviderBundle, String> implements Repository<ProviderBundle, String> {

    public ProviderRepository(GenericResourceService itemService) {
        super(itemService);
    }

    @Override
    public String getResourceTypeName() {
        return "provider";
    }
}
