package gr.madgik.catalogue.openaire.resource;

import eu.einfracentral.domain.Bundle;
import eu.einfracentral.domain.Vocabulary;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.openaire.AbstractBundleService;
import gr.madgik.catalogue.openaire.domain.Service;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.openaire.resource.repository.ServiceRepository;
import gr.madgik.catalogue.repository.RegistryCoreRepository;
import gr.madgik.catalogue.service.VocabularyService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
public class ServiceBundleService extends AbstractBundleService<Service, ServiceBundle, String>
        implements BundleResourceOperations<Service, ServiceBundle, String> {

    private final Catalogue<ServiceBundle, String> catalogue;
    private final RegistryCoreRepository<ServiceBundle, String> repository;
    private final VocabularyService vocabularyService;

    public ServiceBundleService(Catalogue<ServiceBundle, String> catalogue,
                                ServiceRepository repository,
                                VocabularyService vocabularyService) {
        super(catalogue, repository);
        this.catalogue = catalogue;
        this.repository = repository;
        this.vocabularyService = vocabularyService;
    }


    @Override
    public Service register(Service service) {
        return catalogue.register(new ServiceBundle(service)).getPayload();
    }

    @Override
    public ServiceBundle verify(String id, String status, Boolean active) {
        ServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        return repository.update(id, bundle);
    }

    @Override
    public ServiceBundle activate(String id, Boolean active) {
        ServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        return repository.update(id, bundle);
    }

    public Map<String, List<Service>> getByVocabulary(String field, String type) {
        Map<String, List<Service>> map = new HashMap<>();
        List<Vocabulary> vocabularies = vocabularyService.getByType(type);
        FacetFilter filter = new FacetFilter();
        for (Vocabulary vocabulary : vocabularies) {
            filter.addFilter(field, vocabulary.getId());
            map.put(vocabulary.getName(), get(filter).map(Bundle::getPayload).getResults());
        }
        return map;
    }
}
