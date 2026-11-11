package eu.openaire.catalogue.config;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIREModelResourceTypeMapperTest {

    private final OpenAIREModelResourceTypeMapper mapper = new OpenAIREModelResourceTypeMapper();

    private List<IndexField> fields(String name) {
        ResourceType type = new ResourceType();
        type.setName(name);
        return mapper.additionalIndexFields(null, type);
    }

    private IndexField field(String type, String name) {
        return fields(type).stream().filter(f -> name.equals(f.getName())).findFirst().orElseThrow();
    }

    @Test
    void recordsAreReadAsMaps() {
        for (String name : List.of("provider", "service", "datasource")) {
            ResourceType type = new ResourceType();
            type.setName(name);
            assertEquals(LinkedHashMap.class.getName(), mapper.customResourceTypeProperties(type).get("class"));
        }
    }

    @Test
    void indexesTheBundleEnvelope() {
        assertEquals("$.status", field("datasource", "status").getPath());
        assertEquals("$.active", field("provider", "active").getPath());
        assertEquals("$.metadata.registeredAt", field("service", "registeredAt").getPath());
    }

    @Test
    void addsAMultivaluedChannelsFieldToEveryRecordType() {
        for (String type : List.of("provider", "service", "datasource")) {
            IndexField field = field(type, "channels");
            assertTrue(field.isMultivalued(), type);
            assertEquals("$.properties.channels[*]", field.getPath(), type);
        }
    }

    @Test
    void leavesOtherTypesAlone() {
        ResourceType type = new ResourceType();
        type.setName("vocabulary");
        assertTrue(fields("vocabulary").isEmpty());
        assertTrue(mapper.customResourceTypeProperties(type).isEmpty());
    }
}
