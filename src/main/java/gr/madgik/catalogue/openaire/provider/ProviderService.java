package gr.madgik.catalogue.openaire.provider;

import eu.einfracentral.domain.LoggingInfo;
import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.User;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Paging;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.SecurityService;
import gr.madgik.catalogue.openaire.AbstractResourceBundleMongoService;
import gr.madgik.catalogue.openaire.provider.repository.ProviderRepository;
import gr.madgik.catalogue.utils.PagingUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ProviderService extends AbstractResourceBundleMongoService<Provider, ProviderBundle, String> implements BundleResourceOperations<Provider, ProviderBundle, String> {

    private final Catalogue<ProviderBundle, String> catalogue;
    private final ProviderRepository repository;
    private final SecurityService securityService;

    @Value("${project.catalogue.name}")
    private String catalogueName;

    public ProviderService(Catalogue<ProviderBundle, String> catalogue,
                           ProviderRepository repository,
                           SecurityService securityService,
                           MongoTemplate mongoTemplate) {
        super(ProviderBundle.class, mongoTemplate);
        this.catalogue = catalogue;
        this.repository = repository;
        this.securityService = securityService;

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
    public ProviderBundle create(ProviderBundle resource) {
        return repository.create(resource);
    }

    @Override
    public Provider update(String id, Provider provider) {
        ProviderBundle bundle = repository.get(id);
        bundle.setProvider(provider);
        return catalogue.update(id, bundle).getProvider();
    }

    @Override
    public void delete(String id) {
        catalogue.delete(id);
    }

    @Override
    public Provider get(String id) {
        return repository.get(id).getProvider();
    }

    @Override
    public Page<Provider> get(Pageable pageable) {
        return repository.findAll(pageable).map(ProviderBundle::getProvider);
    }

//    @Override
//    public Page<Provider> search(Map<String, Object> filters, Pageable pageable) {
//        Query dynamicQuery = new Query().with(pageable);
//        for (Map.Entry<String, Object> filter : filters.entrySet()) {
//            dynamicQuery.addCriteria(Criteria.where(filter.getKey()).is(filter.getValue()));
//        }
//        List<ProviderBundle> filteredResults = mongoTemplate.find(dynamicQuery, ProviderBundle.class, "providerBundle");
//        Page<ProviderBundle> providerPage = PageableExecutionUtils.getPage(
//                filteredResults,
//                pageable,
//                () -> mongoTemplate.count(dynamicQuery, ProviderBundle.class));
//        return providerPage.map(ProviderBundle::getProvider);
//    }

    @Override
    public Page<Provider> get(FacetFilter filter) {
        return repository.findAll(PagingUtils.toPageable(filter)).map(ProviderBundle::getProvider);
//        return repository.get(filter).map(ProviderBundle::getProvider);
    }

    @Override
    public ProviderBundle verify(String id, String status, Boolean active) {
        ProviderBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        return catalogue.update(id, bundle);
    }

    @Override
    public ProviderBundle activate(String id, Boolean active) {
        ProviderBundle bundle = repository.get(id);
        bundle.setActive(active);
        return catalogue.update(id, bundle);
    }

    public ProviderBundle onboard(ProviderBundle provider, Authentication auth) {
        // create LoggingInfo
        String catalogueId = provider.getProvider().getCatalogueId();
        List<LoggingInfo> loggingInfoList = new ArrayList<>();
//        loggingInfoList.add(LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
//                LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.REGISTERED.getKey()));
        provider.setLoggingInfo(loggingInfoList);
        if (catalogueId == null) {
            // set catalogueId = eosc
            provider.getProvider().setCatalogueId(catalogueName);
            provider.setActive(false);
//            provider.setStatus(vocabularyService.get("pending provider").getId());
//            provider.setTemplateStatus(vocabularyService.get("no template status").getId());
            provider.setStatus("pending provider");
            provider.setTemplateStatus("no template status");
        } else {
            checkCatalogueIdConsistency(provider, catalogueId);
            provider.setActive(true);
//            provider.setStatus(vocabularyService.get("approved provider").getId());
//            provider.setTemplateStatus(vocabularyService.get("approved template").getId());
            provider.setStatus("approved provider");
            provider.setTemplateStatus("approved template");
//            loggingInfoList.add(LoggingInfo.createLoggingInfoEntry(User.of(auth).getEmail(), User.of(auth).getFullName(), securityService.getRoleName(auth),
//                    LoggingInfo.Types.ONBOARD.getKey(), LoggingInfo.ActionType.APPROVED.getKey()));
        }

        // latestOnboardingInfo
//        provider.setLatestOnboardingInfo(loggingInfoList.get(loggingInfoList.size()-1));

        return provider;
    }

    private void checkCatalogueIdConsistency(ProviderBundle provider, String catalogueId){
//        catalogueService.existsOrElseThrow(catalogueId); // FIXME
        if (provider.getProvider().getCatalogueId() == null || provider.getProvider().getCatalogueId().equals("")){
//            throw new ValidationException("Provider's 'catalogueId' cannot be null or empty");
            throw new RuntimeException("Provider's 'catalogueId' cannot be null or empty");
        } else{
            if (!provider.getProvider().getCatalogueId().equals(catalogueId)){
//                throw new ValidationException("Parameter 'catalogueId' and Provider's 'catalogueId' don't match");
                throw new RuntimeException("Parameter 'catalogueId' and Provider's 'catalogueId' don't match");
            }
        }
    }
}
