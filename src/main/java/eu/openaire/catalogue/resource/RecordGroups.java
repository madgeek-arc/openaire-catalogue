package eu.openaire.catalogue.resource;

import gr.uoa.di.madgik.catalogue.domain.SnippetTemplate;
import gr.uoa.di.madgik.catalogue.service.SnippetTemplateCache;
import gr.uoa.di.madgik.registry.domain.Facet;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Value;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.service.FacetLabelService;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

/**
 * Groups records by the values of one of their facets, and reduces payloads to snippets.
 */
@Service
public class RecordGroups {

    private static final String NAME = "name";
    /**
     * Payload fields every snippet keeps, whatever the model says.
     */
    private static final List<String> SNIPPET_KEYS = List.of(Bundles.ID, NAME);

    private final GenericResourceService resources;
    private final ResourceTypeService resourceTypes;
    private final FacetLabelService labels;
    private final SnippetTemplateCache snippetTemplateCache;

    public RecordGroups(GenericResourceService resources, ResourceTypeService resourceTypes, FacetLabelService labels,
                        SnippetTemplateCache snippetTemplateCache) {
        this.resources = resources;
        this.resourceTypes = resourceTypes;
        this.labels = labels;
        this.snippetTemplateCache = snippetTemplateCache;
    }

    /**
     * Groups the records matching the filter by each value of a facet. Groups are keyed by the label of the value
     * (the vocabulary name, for vocabulary fields), or by the value itself when it has no label; values sharing a
     * label share a group. A record with several values is in several groups. Groups are in label order and their
     * records in name order.
     * <p>
     * The search index returns a limited number of groups, and of records per group.
     *
     * @return the bundles of each group
     * @throws ResourceException with status 400 if the field is not a facet of the record type
     */
    public Map<String, List<Map<String, Object>>> group(ResourceKind kind, FacetFilter filter, String field) {
        requireFacet(kind, field);
        filter.setResourceType(kind.type());
        Map<String, List<Map<String, Object>>> byValue = resources.getResultsGrouped(filter, field);
        Map<String, String> labelOf = labels(kind, field, byValue.keySet());

        Map<String, Map<String, Map<String, Object>>> byLabel = new TreeMap<>(
                Comparator.comparing((String label) -> label.toLowerCase()).thenComparing(Comparator.naturalOrder()));
        byValue.forEach((value, bundles) -> {
            Map<String, Map<String, Object>> group = byLabel.computeIfAbsent(labelOf.get(value),
                    label -> new LinkedHashMap<>());
            bundles.forEach(bundle -> group.putIfAbsent(Bundles.id(bundle), bundle));
        });

        Comparator<Map<String, Object>> byName = Comparator.comparing(
                bundle -> Objects.toString(Bundles.payload(bundle, kind).get(NAME), ""), String.CASE_INSENSITIVE_ORDER);
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        byLabel.forEach((label, group) -> grouped.put(label, group.values().stream().sorted(byName).toList()));
        return grouped;
    }

    /**
     * Reduces payloads of the record type to their id, their name, and the fields its model includes in snippets.
     *
     * @throws ResourceException with status 500 if no model defines the record type
     */
    public UnaryOperator<Map<String, Object>> snippets(ResourceKind kind) {
        SnippetTemplate template = snippetTemplateCache.find(kind.type()).orElseThrow(() -> new ResourceException(
                "No model defines " + kind.type() + " records", HttpStatus.INTERNAL_SERVER_ERROR));
        return payload -> {
            Map<String, Object> snippet = new LinkedHashMap<>();
            SNIPPET_KEYS.stream().filter(payload::containsKey).forEach(key -> snippet.put(key, payload.get(key)));
            snippet.putAll(template.apply(payload));
            return snippet;
        };
    }

    private void requireFacet(ResourceKind kind, String field) {
        boolean facet = resourceTypes.getResourceTypeIndexFields(kind.type()).stream()
                .anyMatch(indexField -> indexField.getName().equals(field) && indexField.getLabel() != null
                        && indexField.hasSearchCapability(SearchCapability.KEYWORD));
        if (!facet) {
            throw new ResourceException("'" + field + "' is not a facet of " + kind.type() + " records",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * @return the label of each value, or the value itself when it has none
     */
    private Map<String, String> labels(ResourceKind kind, String field, Collection<String> values) {
        List<Value> facetValues = values.stream().map(value -> new Value(value, 0)).toList();
        labels.enrichFacetLabels(List.of(new Facet(field, null, facetValues)), kind.type());
        Map<String, String> labelOf = new HashMap<>();
        facetValues.forEach(value -> labelOf.put(value.getValue(),
                value.getLabel() != null ? value.getLabel() : value.getValue()));
        return labelOf;
    }
}
