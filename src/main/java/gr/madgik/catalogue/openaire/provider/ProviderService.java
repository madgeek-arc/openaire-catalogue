package gr.madgik.catalogue.openaire.provider;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.openaire.AbstractBundleService;
import gr.madgik.catalogue.openaire.provider.repository.ProviderRepository;
import gr.madgik.catalogue.openaire.utils.ProviderResourcesCommonMethods;
import gr.madgik.catalogue.repository.Repository;
import org.springframework.stereotype.Service;

@Service
public class ProviderService extends AbstractBundleService<Provider, ProviderBundle, String>
        implements BundleResourceOperations<Provider, ProviderBundle, String> {

    private final Catalogue<ProviderBundle, String> catalogue;
    private final Repository<ProviderBundle, String> repository;
    private final ProviderResourcesCommonMethods commonMethods;

    public ProviderService(Catalogue<ProviderBundle, String> catalogue,
                           ProviderRepository repository,
                           ProviderResourcesCommonMethods commonMethods) {
        super(catalogue, repository);
        this.catalogue = catalogue;
        this.repository = repository;
        this.commonMethods = commonMethods;
    }

    @Override
    public boolean validate(Object resource) {
//        throw new UnsupportedOperationException("Not implemented yet");
        return true;
    }

    @Override
    public Provider register(Provider provider) {
        return catalogue.register(new ProviderBundle(provider)).getProvider();
    }

    @Override
    public ProviderBundle verify(String id, String status, Boolean active) {
        ProviderBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        commonMethods.logVerificationAndActivation(bundle, status, null);
        return repository.update(id, bundle);
    }

    @Override
    public ProviderBundle activate(String id, Boolean active) {
        ProviderBundle bundle = repository.get(id);
        bundle.setActive(active);
        commonMethods.logVerificationAndActivation(bundle, null, active);
        return repository.update(id, bundle);
    }
}
