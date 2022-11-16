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
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Override
//    @Cacheable(value = CACHE_FOR_UI)
    public Map<String, List<Value>> getControlValuesMap() {
        Map<String, List<Value>> controlValues = new HashMap<>();
        List<Value> values;
        FacetFilter providerFilter = new FacetFilter();
        providerFilter.setQuantity(10000);

        // add providers
        values = this.providerService.get(providerFilter).getResults()
                .parallelStream()
                .map(value -> new Value(value.getId(), value.getPayload().getName()))
                .collect(Collectors.toList());
        controlValues.put("Provider", values);

        // add Resource Organizations from EOSC
        controlValues.put("resourceProviders", getEoscProviders());

        // add services
        FacetFilter serviceFilter = new FacetFilter();
        serviceFilter.setQuantity(10000);
        serviceFilter.addFilter("active", "true");
        values = this.serviceService.get(serviceFilter).getResults()
                .parallelStream()
                .map(value -> new Value(value.getId(), value.getPayload().getName()))
                .collect(Collectors.toList());
        controlValues.put("Service", values);


        // add all vocabularies
        for (Map.Entry<String, List<Vocabulary>> entry : this.getByType().entrySet()) {
            values = entry.getValue()
                    .stream()
                    .map(v -> new Value(v.getId(), v.getName(), v.getParentId()))
                    .collect(Collectors.toList());
            controlValues.put(entry.getKey(), values);
        }

        return controlValues;
    }

    private List<Value> getEoscProviders() {
        RestTemplate restTemplate = new RestTemplate();
        Browsing<Map<String, String>> eoscProviders;
        eoscProviders = restTemplate.getForObject("https://providers.eosc-portal.eu/api/provider/all?status=approved provider&quantity=1000", Browsing.class);
        List<Map<String, String>> providerList = new ArrayList<>(eoscProviders != null && eoscProviders.getResults() != null ? eoscProviders.getResults() : new ArrayList<>());

        return providerList.stream()
                .map(value -> new Value(value.get("id"), value.get("name")))
                .collect(Collectors.toList());
    }
}
