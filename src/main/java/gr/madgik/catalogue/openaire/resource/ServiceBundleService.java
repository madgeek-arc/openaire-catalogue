package gr.madgik.catalogue.openaire.resource;

import eu.einfracentral.domain.Vocabulary;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.Catalogue;
import gr.madgik.catalogue.openaire.AbstractResourceBundleMongoService;
import gr.madgik.catalogue.openaire.OpenAIREService;
import gr.madgik.catalogue.openaire.OpenAIREServiceBundle;
import gr.madgik.catalogue.openaire.resource.repository.ServiceRepository;
import gr.madgik.catalogue.service.VocabularyService;
import gr.madgik.catalogue.utils.PagingUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@org.springframework.stereotype.Service
public class ServiceBundleService extends AbstractResourceBundleMongoService<OpenAIREService, OpenAIREServiceBundle, String> implements BundleResourceOperations<OpenAIREService, OpenAIREServiceBundle, String> {

    private final Catalogue<OpenAIREServiceBundle, String> catalogue;
    private final ServiceRepository repository;
    private final VocabularyService vocabularyService;

    public ServiceBundleService(Catalogue<OpenAIREServiceBundle, String> catalogue,
                                ServiceRepository repository,
                                VocabularyService vocabularyService,
                                MongoTemplate mongoTemplate) {
        super(OpenAIREServiceBundle.class, mongoTemplate);
        this.catalogue = catalogue;
        this.repository = repository;
        this.vocabularyService = vocabularyService;
    }

    @Override
    public boolean validate(Object resource) {
        throw new UnsupportedOperationException("Not implemented yet");
//        return null;
    }

    @Override
    public OpenAIREService register(OpenAIREService service) {
        return catalogue.register(new OpenAIREServiceBundle(service)).getPayload();
    }

    @Override
    public OpenAIREServiceBundle create(OpenAIREServiceBundle resource) {
        return repository.create(resource);
    }

    @Override
    public OpenAIREService update(String id, OpenAIREService service) {
        OpenAIREServiceBundle bundle = repository.get(id);
        bundle.setPayload(service);
        return catalogue.update(id, bundle).getPayload();
    }

    @Override
    public void delete(String id) {
        catalogue.delete(id);
    }

    @Override
    public OpenAIREService get(String id) {
        OpenAIREServiceBundle bundle = repository.get(id);
        return bundle != null ? bundle.getPayload() : null;
    }

    @Override
    public Page<OpenAIREService> get(Pageable pageable) {
        return repository.get(pageable).map(OpenAIREServiceBundle::getService);
    }

    @Override
    public Page<OpenAIREService> get(FacetFilter filter) {
//        Paging<OpenAIREServiceBundle> bundlePaging = repository.get(filter);
//        Paging<OpenAIREService> services = new Paging<>(bundlePaging, bundlePaging.getResults().stream().filter(Objects::nonNull).map(OpenAIREServiceBundle::getService).collect(Collectors.toList()));
//        return services;
        return repository.get(PagingUtils.toPageable(filter)).map(OpenAIREServiceBundle::getService);
    }

    @Override
    public OpenAIREServiceBundle verify(String id, String status, Boolean active) {
        OpenAIREServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        bundle.setStatus(status);
        return catalogue.update(id, bundle);
    }

    @Override
    public OpenAIREServiceBundle activate(String id, Boolean active) {
        OpenAIREServiceBundle bundle = repository.get(id);
        bundle.setActive(active);
        return catalogue.update(id, bundle);
    }

    public Map<String, List<OpenAIREService>> getByVocabulary(String field, Vocabulary.Type type) {
        Map<String, List<OpenAIREService>> map = new HashMap<>();
        List<Vocabulary> vocabularies = vocabularyService.getByType(type);
        Map<String, Object> filter = new HashMap<>();
        for (Vocabulary vocabulary : vocabularies) {
            filter.put(field, vocabulary.getId());
            map.put(vocabulary.getName(), search(filter));
        }
        return map;
    }
}
