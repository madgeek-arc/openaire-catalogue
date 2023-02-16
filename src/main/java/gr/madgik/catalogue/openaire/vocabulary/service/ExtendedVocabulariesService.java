package gr.madgik.catalogue.openaire.vocabulary.service;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.ProviderBundle;
import eu.einfracentral.domain.Vocabulary;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.madgik.catalogue.BundleResourceOperations;
import gr.madgik.catalogue.dto.Value;
import gr.madgik.catalogue.openaire.domain.Service;
import gr.madgik.catalogue.openaire.domain.ServiceBundle;
import gr.madgik.catalogue.repository.VocabularyRepository;
import gr.madgik.catalogue.service.VocabularyService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gr.madgik.catalogue.openaire.config.CacheConfig.CACHE_UI_VOCABULARIES;

@org.springframework.stereotype.Service
public class ExtendedVocabulariesService extends VocabularyService implements ExtendedVocabularyOperations {

    private final BundleResourceOperations<Service, ServiceBundle, String> serviceService;
    private final BundleResourceOperations<Provider, ProviderBundle, String> providerService;

    public ExtendedVocabulariesService(VocabularyRepository vocabularyRepository,
                                       BundleResourceOperations<Service, ServiceBundle, String> serviceService,
                                       BundleResourceOperations<Provider, ProviderBundle, String> providerService) {
        super(vocabularyRepository);
        this.serviceService = serviceService;
        this.providerService = providerService;
    }

    @Scheduled(fixedRate = 600000) // refresh cache every 10 minutes
    @CachePut(value = CACHE_UI_VOCABULARIES)
    public Map<String, List<Value>> cacheVocabularies() {
        return getControlValuesMap();
    }

    @Override
    @Cacheable(value = CACHE_UI_VOCABULARIES)
    public Map<String, List<Value>> getControlValuesMap() {
        Map<String, List<Value>> controlValues = new HashMap<>();
        List<Value> values;

        // add providers
        controlValues.put("Provider", getAllProviders());

        // add services
        controlValues.put("Service", getAllServices());

        // add Resource Organizations from EOSC
        controlValues.put("resourceProviders", controlValues.get("Provider"));

        // add all vocabularies
        for (Map.Entry<String, List<Vocabulary>> entry : this.getByType().entrySet()) {
            values = entry.getValue()
                    .stream()
                    .map(v -> new Value(v.getId(), v.getName(), v.getParentId()))
                    .toList();
            controlValues.put(entry.getKey(), values);
        }

        return controlValues;
    }

    List<Value> getAllProviders() {
        FacetFilter providerFilter = new FacetFilter();
        providerFilter.setQuantity(10000);
        return this.providerService.get(providerFilter).getResults()
                .stream()
                .map(value -> new Value(value.getId(), value.getPayload().getName()))
                .toList();
    }

    List<Value> getAllServices() {
        FacetFilter serviceFilter = new FacetFilter();
        serviceFilter.setQuantity(10000);
        serviceFilter.addFilter("active", "true");
        return this.serviceService.get(serviceFilter).getResults()
                .stream()
                .map(value -> new Value(value.getId(), value.getPayload().getName()))
                .toList();
    }

    @Deprecated
    List<Value> getEoscProviders() {
        RestTemplate restTemplate = new RestTemplate();
        Browsing<Map<String, String>> eoscProviders;
        eoscProviders = restTemplate.getForObject("https://providers.eosc-portal.eu/api/provider/all?status=approved provider&quantity=1000", Browsing.class);
        List<Map<String, String>> providerList = new ArrayList<>(eoscProviders != null && eoscProviders.getResults() != null ? eoscProviders.getResults() : new ArrayList<>());

        return providerList.stream()
                .map(value -> new Value(value.get("id"), value.get("name")))
                .toList();
    }
}
