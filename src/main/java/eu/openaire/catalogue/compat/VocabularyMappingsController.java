package eu.openaire.catalogue.compat;

import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.resourcecatalogue.domain.Bundle;
import gr.uoa.di.madgik.resourcecatalogue.domain.Vocabulary;
import gr.uoa.di.madgik.resourcecatalogue.service.OrganisationService;
import gr.uoa.di.madgik.resourcecatalogue.service.ServiceService;
import gr.uoa.di.madgik.resourcecatalogue.service.VocabularyService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Serves every vocabulary plus the lists of providers and services as {@code type name -> values}, the
 * format consumed by OpenAIRE form models. Results are cached for ten minutes.
 */
@RestController
public class VocabularyMappingsController {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final int MAX_RESULTS = 10_000;

    public record MappingValue(String id, String name, String parentId) {
    }

    private final VocabularyService vocabularyService;
    private final OrganisationService organisationService;
    private final ServiceService serviceService;

    private volatile Map<String, List<MappingValue>> cache;
    private volatile long cachedAt;

    public VocabularyMappingsController(VocabularyService vocabularyService,
                                        OrganisationService organisationService,
                                        ServiceService serviceService) {
        this.vocabularyService = vocabularyService;
        this.organisationService = organisationService;
        this.serviceService = serviceService;
    }

    @GetMapping(value = "vocabularies/mappings", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<MappingValue>> getControlValuesByType() {
        Map<String, List<MappingValue>> current = cache;
        if (current == null || System.nanoTime() - cachedAt > TTL.toNanos()) {
            synchronized (this) {
                if (cache == null || System.nanoTime() - cachedAt > TTL.toNanos()) {
                    cache = load();
                    cachedAt = System.nanoTime();
                }
                current = cache;
            }
        }
        return current;
    }

    private Map<String, List<MappingValue>> load() {
        Map<String, List<MappingValue>> values = new LinkedHashMap<>();
        List<MappingValue> providers = bundleValues(() -> organisationService.getAll(filter(false)).getResults());
        values.put("Provider", providers);
        values.put("resourceProviders", providers);
        values.put("Service", bundleValues(() -> serviceService.getAll(filter(true)).getResults()));

        for (Map.Entry<Vocabulary.Type, List<Vocabulary>> entry : vocabularyService.getAllVocabulariesByType().entrySet()) {
            values.put(entry.getKey().getKey(), entry.getValue().stream()
                    .map(v -> new MappingValue(v.getId(), v.getName(), v.getParentId()))
                    .toList());
        }
        return values;
    }

    private static FacetFilter filter(boolean activeOnly) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(MAX_RESULTS);
        ff.addFilter("published", false);
        if (activeOnly) {
            ff.addFilter("active", true);
        }
        return ff;
    }

    private static List<MappingValue> bundleValues(Supplier<? extends List<? extends Bundle>> source) {
        List<MappingValue> values = new ArrayList<>();
        for (Bundle bundle : source.get()) {
            Object name = bundle.getPayload().get("name");
            values.add(new MappingValue(bundle.getId(), name != null ? name.toString() : bundle.getId(), null));
        }
        return values;
    }
}
