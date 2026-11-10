package eu.openaire.catalogue.vocabulary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vocabulary reads, under both {@code vocabulary} and {@code vocabularies}. {@code types/{type}/{id}} answers 200 or
 * 404.
 */
@RestController
@RequestMapping(path = {"vocabulary", "vocabularies"}, produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "vocabulary")
public class VocabularyController {

    private static final Duration MAPPINGS_TTL = Duration.ofMinutes(10);

    public record MappingValue(String id, String name, String parentId) {
    }

    private final VocabularyService vocabularies;
    private volatile Map<String, List<MappingValue>> mappings;
    private volatile long mappingsLoadedAt;

    public VocabularyController(VocabularyService vocabularies) {
        this.vocabularies = vocabularies;
    }

    @Operation(summary = "Returns the vocabulary entry with the given id.")
    @GetMapping("{id}")
    public Map<String, Object> get(@PathVariable String id) {
        return vocabularies.get(id);
    }

    @Operation(summary = "Returns every vocabulary entry, grouped by type.")
    @GetMapping("byType")
    public Map<String, List<Map<String, Object>>> getAllByType() {
        return vocabularies.allByType();
    }

    @Operation(summary = "Returns the entries of a vocabulary type, sorted by name.")
    @GetMapping({"types/{type}", "byType/{type}"})
    public List<Map<String, Object>> getByType(@PathVariable String type,
                                               @RequestParam(name = "parent_id", required = false) String parentId) {
        return vocabularies.byType(type, parentId);
    }

    @Operation(summary = "Returns the entry of a vocabulary type with the given id.")
    @GetMapping("types/{type}/{id}")
    public Map<String, Object> getByTypeAndId(@PathVariable String type, @PathVariable String id) {
        return vocabularies.byTypeAndId(type, id);
    }

    @Operation(summary = "Returns the entry of a vocabulary type with the given id, for ids containing a '/'.")
    @GetMapping("types/{type}/{prefix}/{suffix}")
    public Map<String, Object> getByTypeAndId(@PathVariable String type, @PathVariable String prefix,
                                              @PathVariable String suffix) {
        return vocabularies.byTypeAndId(type, prefix + "/" + suffix);
    }

    /**
     * Results are cached for ten minutes.
     */
    @Operation(summary = "Returns every vocabulary, with the providers and services, as type name to values: the "
            + "format consumed by the OpenAIRE form models.")
    @GetMapping("mappings")
    public Map<String, List<MappingValue>> getMappings() {
        Map<String, List<MappingValue>> current = mappings;
        if (current == null || System.nanoTime() - mappingsLoadedAt > MAPPINGS_TTL.toNanos()) {
            synchronized (this) {
                if (mappings == null || System.nanoTime() - mappingsLoadedAt > MAPPINGS_TTL.toNanos()) {
                    mappings = loadMappings();
                    mappingsLoadedAt = System.nanoTime();
                }
                current = mappings;
            }
        }
        return current;
    }

    private Map<String, List<MappingValue>> loadMappings() {
        Map<String, List<MappingValue>> values = new LinkedHashMap<>();
        vocabularies.allByType().forEach((type, entries) -> values.put(type, entries.stream()
                .map(entry -> new MappingValue(text(entry.get("id")), text(entry.get("name")),
                        text(entry.get("parentId"))))
                .toList()));
        return values;
    }

    private static String text(Object value) {
        return value != null ? value.toString() : null;
    }
}
