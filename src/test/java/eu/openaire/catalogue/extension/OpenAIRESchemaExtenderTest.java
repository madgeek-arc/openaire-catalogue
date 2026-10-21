package eu.openaire.catalogue.extension;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIRESchemaExtenderTest {

    private final OpenAIRESchemaExtender extender = new OpenAIRESchemaExtender();
    private final JsonMapper mapper = JsonMapper.builder().build();

    @SuppressWarnings("unchecked")
    private Map<String, Object> schema(String type) throws Exception {
        try (InputStream in = new ClassPathResource("schemas/json/" + type + "-schema.json").getInputStream()) {
            return mapper.readValue(in, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> props(Map<String, Object> schema) {
        return (Map<String, Object>) schema.get("properties");
    }

    @Test
    void serviceSchemaGainsOpenAIREProperties() throws Exception {
        Map<String, Object> schema = schema("Service");
        assertFalse(props(schema).containsKey("extras"));

        extender.extendSchema("service", schema);

        assertTrue(props(schema).containsKey("extras"));
        assertTrue(props(schema).containsKey("lifeCycleStatus"));
        assertTrue(props(schema).containsKey("resourceOrganisation"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mainContact = (Map<String, Object>) props(schema).get("mainContact");
        assertTrue(props(mainContact).containsKey("phone"));
        assertFalse(schema.isEmpty());
    }

    @Test
    void extensionIsIdempotent() throws Exception {
        Map<String, Object> once = schema("Service");
        extender.extendSchema("service", once);
        String first = mapper.writeValueAsString(once);
        extender.extendSchema("service", once);
        assertEquals(first, mapper.writeValueAsString(once));
    }

    @Test
    void datasourceSchemaGainsExtrasAndHarvestable() throws Exception {
        Map<String, Object> schema = schema("Datasource");
        extender.extendSchema("datasource", schema);
        assertTrue(props(schema).containsKey("extras"));
        assertTrue(props(schema).containsKey("harvestable"));
    }

    @Test
    void indexFieldsAreAddedOnce() {
        ResourceType type = new ResourceType();
        type.setName("service");
        extender.extendResourceType(type);
        int size = type.getIndexFields().size();
        extender.extendResourceType(type);
        assertEquals(size, type.getIndexFields().size());
        assertTrue(type.getIndexFields().stream().anyMatch(f -> "portfolios".equals(f.getName())));
    }
}
