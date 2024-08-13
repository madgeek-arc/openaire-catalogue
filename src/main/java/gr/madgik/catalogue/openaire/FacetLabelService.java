package gr.madgik.catalogue.openaire;

import gr.madgik.catalogue.openaire.domain.ProviderBundle;
import gr.madgik.catalogue.openaire.domain.Vocabulary;
import gr.madgik.catalogue.openaire.vocabulary.service.VocabularyOperations;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.athenarc.catalogue.service.GenericItemService;
import gr.uoa.di.madgik.registry.domain.Paging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
// TODO: move functionality to registry-core
// add "resourceType" as a field of each indexed field (when refers to a vocabulary) for easier searching
public class FacetLabelService {

    private static final Logger logger = LoggerFactory.getLogger(FacetLabelService.class);
    private final GenericItemService genericItemService;
    private final VocabularyOperations vocabularyOperations;

    @org.springframework.beans.factory.annotation.Value("${elastic.index.max_result_window:10000}")
    private int maxQuantity;

    @Autowired
    FacetLabelService(GenericItemService genericItemService, @Qualifier("vocabularyService") VocabularyOperations vocabularyOperations) {
        this.genericItemService = genericItemService;
        this.vocabularyOperations = vocabularyOperations;
    }

    String toProperCase(String str, String delimiter, String newDelimiter) {
        if (str.equals("")){
            str = "-";
        }
        StringJoiner joiner = new StringJoiner(newDelimiter);
        for (String s : str.split(delimiter)) {
            String s1;
            s1 = s.substring(0, 1).toUpperCase() + s.substring(1);
            joiner.add(s1);
        }
        return joiner.toString();
    }

    public List<Facet> createLabels(List<Facet> facets) {
        FacetFilter ff = new FacetFilter();
        ff.setQuantity(maxQuantity);
        ff.setResourceType("provider");
        // TODO: get all final providers (after deduplication process)
        Paging<ProviderBundle> allProviders = genericItemService.getResults(ff);
        Map<String, String> vocabularyValues = new TreeMap<>();
        vocabularyValues.putAll(allProviders.getResults().stream().collect(Collectors.toMap(ProviderBundle::getId, res -> res.getProvider().getName())));

        FacetFilter vocFilter = new FacetFilter();
        vocFilter.setQuantity(maxQuantity);
        vocabularyValues.putAll(vocabularyOperations.get(vocFilter)
                .getResults()
                .stream()
                .collect(Collectors.toMap(Vocabulary::getId, Vocabulary::getName)));

        for (Facet facet : facets) {
            facet.getValues().forEach(value -> value.setLabel(getLabelElseKeepValue(value.getValue(), vocabularyValues)));
        }
        return facets;
    }

    String getLabelElseKeepValue(String value, Map<String, String> labels) {
        String ret = labels.get(value);
        if (ret == null) {
            ret = toProperCase(toProperCase(value, "-", "-"), "_", " ");
        }
        return ret;
    }
}
