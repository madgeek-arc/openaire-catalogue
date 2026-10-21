package gr.madgik.catalogue.openaire.extension;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import gr.uoa.di.madgik.resourcecatalogue.extension.SchemaExtender;
import gr.uoa.di.madgik.resourcecatalogue.extension.SchemaExtensions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds the OpenAIRE service and datasource properties, including the free-form {@code extras} object,
 * to the service and datasource schemas and index fields.
 * Properties already present in the schema are left untouched; only missing ones are added.
 */
@Component
public class OpenAIRESchemaExtender implements SchemaExtender {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final String SERVICE = "service";
    private static final String DATASOURCE = "datasource";

    private record IndexSpec(String name, String label, String property, boolean multivalued) {
    }

    private static final Map<String, List<IndexSpec>> INDEX_FIELDS = Map.of(
            SERVICE, List.of(
                    new IndexSpec("abbreviation", "Abbreviation", "abbreviation", false),
                    new IndexSpec("resource_organisation", "Resource Organisation", "resourceOrganisation", false),
                    new IndexSpec("resource_providers", "Resource Providers", "resourceProviders[*]", true),
                    new IndexSpec("target_users", "Target Users", "targetUsers[*]", true),
                    new IndexSpec("access_modes", "Access Modes", "accessModes[*]", true),
                    new IndexSpec("geographical_availabilities", "Geographical Availabilities",
                            "geographicalAvailabilities[*]", true),
                    new IndexSpec("language_availabilities", "Language Availabilities",
                            "languageAvailabilities[*]", true),
                    new IndexSpec("life_cycle_status", "Life Cycle Status", "lifeCycleStatus", false),
                    new IndexSpec("funding_body", "Funding Body", "fundingBody[*]", true),
                    new IndexSpec("funding_programs", "Funding Programs", "fundingPrograms[*]", true),
                    new IndexSpec("portfolios", "Portfolios", "extras.portfolios[*]", true),
                    new IndexSpec("users", "Users", "extras.users[*]", true)
            ),
            DATASOURCE, List.of(
                    new IndexSpec("harvestable", "Harvestable", "harvestable", false),
                    new IndexSpec("research_entity_types", "Research Entity Types", "researchEntityTypes[*]", true),
                    new IndexSpec("portfolios", "Portfolios", "extras.portfolios[*]", true),
                    new IndexSpec("users", "Users", "extras.users[*]", true)
            )
    );

    @Override
    public boolean supports(String resourceType) {
        return SERVICE.equals(resourceType) || DATASOURCE.equals(resourceType);
    }

    @Override
    public void extendSchema(String resourceType, Map<String, Object> schema) {
        Map<String, Object> extension = readExtension(resourceType);
        mergeMissing(schema, extension);
        if (DATASOURCE.equals(resourceType)) {
            Map<String, Object> extras = new LinkedHashMap<>();
            extras.put("type", "object");
            SchemaExtensions.addProperties(schema, Map.of("extras", extras));
        }
        allowAdditionalProperties(schema, "extras");
        SchemaExtensions.addProperties(schema, Map.of("resourceOrganisation", Map.of("type", "string")));
    }

    @Override
    public void extendResourceType(ResourceType resourceType) {
        List<IndexSpec> specs = INDEX_FIELDS.get(resourceType.getName());
        if (specs == null) {
            return;
        }
        List<IndexField> fields = resourceType.getIndexFields();
        for (IndexSpec spec : specs) {
            if (fields.stream().anyMatch(f -> spec.name().equals(f.getName()))) {
                continue;
            }
            IndexField field = new IndexField();
            field.setName(spec.name());
            field.setLabel(spec.label());
            field.setPath("$." + resourceType.getName() + "." + spec.property());
            field.setType("java.lang.String");
            field.setMultivalued(spec.multivalued());
            field.setSearchCapabilities(EnumSet.of(SearchCapability.KEYWORD));
            field.setEmbeddingWeight(0.0f);
            field.setResourceType(resourceType);
            fields.add(field);
        }
        resourceType.setIndexFields(fields);
    }

    private Map<String, Object> readExtension(String resourceType) {
        ClassPathResource resource = new ClassPathResource("openaire/" + resourceType + "-schema-extension.json");
        try (InputStream in = resource.getInputStream()) {
            return MAPPER.readValue(in, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read schema extension of " + resourceType, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void allowAdditionalProperties(Map<String, Object> schema, String property) {
        if (schema.get("properties") instanceof Map<?, ?> props
                && props.get(property) instanceof Map<?, ?> target) {
            ((Map<String, Object>) target).remove("additionalProperties");
        }
    }

    /**
     * Adds the properties of {@code source} that {@code target} lacks, descending into object
     * properties and array items that exist on both sides.
     */
    @SuppressWarnings("unchecked")
    private static void mergeMissing(Map<String, Object> target, Map<String, Object> source) {
        if (!(source.get("properties") instanceof Map<?, ?> sourceProps)) {
            return;
        }
        Object existing = target.get("properties");
        Map<String, Object> targetProps;
        if (existing instanceof Map<?, ?>) {
            targetProps = (Map<String, Object>) existing;
        } else {
            targetProps = new LinkedHashMap<>();
            target.put("properties", targetProps);
        }
        for (Map.Entry<?, ?> entry : sourceProps.entrySet()) {
            String name = (String) entry.getKey();
            Map<String, Object> sourceProp = (Map<String, Object>) entry.getValue();
            Object targetProp = targetProps.get(name);
            if (targetProp == null) {
                targetProps.put(name, sourceProp);
            } else if (targetProp instanceof Map<?, ?> tp) {
                mergeProperty((Map<String, Object>) tp, sourceProp);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void mergeProperty(Map<String, Object> target, Map<String, Object> source) {
        if ("object".equals(target.get("type")) && "object".equals(source.get("type"))) {
            mergeMissing(target, source);
        } else if ("array".equals(target.get("type")) && "array".equals(source.get("type"))
                && target.get("items") instanceof Map<?, ?> targetItems
                && source.get("items") instanceof Map<?, ?> sourceItems) {
            mergeProperty((Map<String, Object>) targetItems, (Map<String, Object>) sourceItems);
        }
    }
}
