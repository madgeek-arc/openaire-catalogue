package eu.openaire.catalogue.vocabulary;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Bundles;
import eu.openaire.catalogue.resource.ResourceKind;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.GenericResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vocabulary entries, kept as JSON maps in the {@code vocabulary} resource type, and two kinds of derived types:
 * reference types whose values are records ({@code Provider} and {@code resourceProviders} list the providers,
 * {@code Service} the active services), and combined types whose values are the entries of several types
 * ({@code Geographical availability} is a region or a country, as lot1 allows for that field).
 */
@Service
public class VocabularyService {

    public static final String RESOURCE_TYPE = "vocabulary";
    private static final Logger logger = LoggerFactory.getLogger(VocabularyService.class);
    private static final int MAX_QUANTITY = 10_000;
    private static final Map<String, ResourceKind> REFERENCE_TYPES = Map.of(
            "Provider", ResourceKind.PROVIDER,
            "resourceProviders", ResourceKind.PROVIDER,
            "Service", ResourceKind.SERVICE);
    private static final Map<String, List<String>> COMBINED_TYPES = Map.of(
            "Geographical availability", List.of("Region", "Country"));

    private final GenericResourceService resources;
    private final BundleService bundles;

    public VocabularyService(GenericResourceService resources, BundleService bundles) {
        this.resources = resources;
        this.bundles = bundles;
    }

    /**
     * @throws ResourceNotFoundException if no entry has the id
     */
    public Map<String, Object> get(String id) {
        return resources.get(RESOURCE_TYPE, id);
    }

    /**
     * @param parentId only entries with this parent, when given
     * @return the entries of a type, sorted by name
     */
    public List<Map<String, Object>> byType(String type, String parentId) {
        ResourceKind reference = REFERENCE_TYPES.get(type);
        if (reference != null) {
            return references(reference);
        }
        List<String> combined = COMBINED_TYPES.get(type);
        if (combined != null) {
            List<Map<String, Object>> entries = new ArrayList<>();
            combined.forEach(part -> entries.addAll(byType(part, null)));
            return entries;
        }
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(RESOURCE_TYPE);
        filter.setQuantity(MAX_QUANTITY);
        filter.addFilter("type", type);
        if (parentId != null && !parentId.isBlank()) {
            filter.addFilter("parent_id", parentId);
        }
        List<Map<String, Object>> entries = new ArrayList<>(read(filter));
        entries.sort(Comparator.comparing(entry -> String.valueOf(entry.get("name"))));
        return entries;
    }

    /**
     * @return the entry with the id, if it belongs to the type
     * @throws ResourceNotFoundException if the type has no such entry
     */
    public Map<String, Object> byTypeAndId(String type, String id) {
        ResourceKind reference = REFERENCE_TYPES.get(type);
        if (reference != null) {
            Map<String, Object> bundle = bundles.get(reference, id);
            if (reference == ResourceKind.SERVICE && !Bundles.active(bundle)) {
                throw new ResourceNotFoundException(id, type);
            }
            return entry(reference, bundle);
        }
        Map<String, Object> entry = get(id);
        if (!COMBINED_TYPES.getOrDefault(type, List.of(type)).contains(String.valueOf(entry.get("type")))) {
            throw new ResourceNotFoundException(id, type);
        }
        return entry;
    }

    /**
     * @return every entry, grouped by type, the reference types included
     */
    public Map<String, List<Map<String, Object>>> allByType() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        REFERENCE_TYPES.keySet().stream().sorted().forEach(type -> result.put(type, byType(type, null)));
        COMBINED_TYPES.keySet().stream().sorted().forEach(type -> result.put(type, byType(type, null)));
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(RESOURCE_TYPE);
        filter.setQuantity(MAX_QUANTITY);
        for (Map<String, Object> entry : read(filter)) {
            result.computeIfAbsent(String.valueOf(entry.get("type")), key -> new ArrayList<>()).add(entry);
        }
        result.values().forEach(entries -> entries.sort(Comparator.comparing(e -> String.valueOf(e.get("name")))));
        return result;
    }

    /**
     * Stores a new entry.
     *
     * @throws ResourceException if the entry is not valid, see {@link #validate(Map)}
     */
    public Map<String, Object> add(Map<String, Object> entry) {
        return resources.add(RESOURCE_TYPE, validate(entry), true);
    }

    /**
     * Stores several new entries, in order. Entries before an invalid one stay stored.
     */
    public List<Map<String, Object>> addAll(List<Map<String, Object>> entries) {
        return entries.stream().map(this::add).toList();
    }

    /**
     * Replaces the entry with the id.
     *
     * @throws ResourceException         if the entry is not valid, or its id differs from the given one
     * @throws ResourceNotFoundException if no entry has the id
     */
    public Map<String, Object> update(String id, Map<String, Object> entry) {
        Map<String, Object> updated = new LinkedHashMap<>(entry);
        Object payloadId = updated.putIfAbsent("id", id);
        if (payloadId != null && !id.equals(payloadId.toString())) {
            throw new ResourceException("The entry id '" + payloadId + "' differs from '" + id + "'",
                    HttpStatus.BAD_REQUEST);
        }
        get(id);
        return resources.update(RESOURCE_TYPE, validate(updated), true);
    }

    /**
     * @return the deleted entry
     * @throws ResourceNotFoundException if no entry has the id
     */
    public Map<String, Object> delete(String id) {
        return resources.delete(RESOURCE_TYPE, id);
    }

    /**
     * An entry needs an id, a name and a type that is not a reference or combined type; its parent, when given, must
     * exist.
     *
     * @return the entry
     * @throws ResourceException if the entry is not valid
     */
    public Map<String, Object> validate(Map<String, Object> entry) {
        for (String field : List.of("id", "name", "type")) {
            Object value = entry.get(field);
            if (value == null || value.toString().isBlank()) {
                throw new ResourceException("The entry has no " + field, HttpStatus.BAD_REQUEST);
            }
        }
        String type = entry.get("type").toString();
        if (REFERENCE_TYPES.containsKey(type) || COMBINED_TYPES.containsKey(type)) {
            throw new ResourceException("The entries of '" + type + "' are derived and cannot be stored",
                    HttpStatus.BAD_REQUEST);
        }
        Object parentId = entry.get("parentId");
        if (parentId != null && !parentId.toString().isBlank()) {
            try {
                get(parentId.toString());
            } catch (ResourceNotFoundException e) {
                throw new ResourceException("The parent entry '" + parentId + "' does not exist",
                        HttpStatus.BAD_REQUEST);
            }
        }
        return entry;
    }

    /**
     * @return the entries matching the filter, up to its quantity; a warning is logged when more match
     */
    private List<Map<String, Object>> read(FacetFilter filter) {
        Paging<Map<String, Object>> results = resources.getResults(filter);
        if (results.getTotal() > results.getResults().size()) {
            logger.warn("Read {} of {} vocabulary entries matching {}", results.getResults().size(), results.getTotal(),
                    filter.getFilter());
        }
        return results.getResults();
    }

    private List<Map<String, Object>> references(ResourceKind kind) {
        Map<String, Object> filters = kind == ResourceKind.SERVICE ? Map.of(Bundles.ACTIVE, true) : Map.of();
        return bundles.all(kind, filters).stream()
                .map(bundle -> entry(kind, bundle))
                .sorted(Comparator.comparing(entry -> String.valueOf(entry.get("name"))))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static Map<String, Object> entry(ResourceKind kind, Map<String, Object> bundle) {
        Object name = Bundles.payload(bundle, kind).get("name");
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", Bundles.id(bundle));
        entry.put("name", name != null ? name.toString() : Bundles.id(bundle));
        entry.put("type", kind.type());
        entry.put("parentId", null);
        return entry;
    }
}
