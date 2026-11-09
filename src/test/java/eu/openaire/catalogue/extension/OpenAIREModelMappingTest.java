package eu.openaire.catalogue.extension;

import gr.uoa.di.madgik.catalogue.domain.Model;
import gr.uoa.di.madgik.catalogue.domain.Section;
import gr.uoa.di.madgik.catalogue.domain.UiField;
import gr.uoa.di.madgik.catalogue.service.ModelResourceTypeMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIREModelMappingTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ModelResourceTypeMapper typeMapper = new ModelResourceTypeMapper();

    @Test
    void serviceModelProducesOpenAIREIndexFields() throws IOException {
        Map<String, IndexField> fields = indexFields("service");

        assertPath(fields, "targetUsers", "$.service.targetUsers[*]");
        assertPath(fields, "resourceOrganisation", "$.service.resourceOrganisation");
        assertPath(fields, "resourceProviders", "$.service.resourceProviders[*]");
        assertPath(fields, "portfolios", "$.service.extras.portfolios[*]");
        assertPath(fields, "users", "$.service.extras.users[*]");
    }

    @Test
    void datasourceModelProducesOpenAIREIndexFields() throws IOException {
        Map<String, IndexField> fields = indexFields("datasource");

        assertPath(fields, "harvestable", "$.datasource.harvestable");
        assertPath(fields, "researchEntityTypes", "$.datasource.researchEntityTypes[*]");
    }

    @Test
    void providerModelProducesOpenAIREIndexFields() throws IOException {
        Map<String, IndexField> fields = indexFields("provider");

        assertPath(fields, "structureTypes", "$.provider.structureTypes[*]");
        assertPath(fields, "esfriType", "$.provider.esfriType");
    }

    @ParameterizedTest
    @ValueSource(strings = {"service", "provider", "datasource"})
    void everyFieldHasALabel(String type) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/openaire/models/m-oa-" + type + ".json")) {
            List<String> unlabelled = new ArrayList<>();
            for (Section section : mapper.readValue(in, Model.class).getSections()) {
                collectUnlabelled(section, unlabelled);
            }
            assertEquals(List.of(), unlabelled);
        }
    }

    private void collectUnlabelled(Section section, List<String> unlabelled) {
        if (section.getSubSections() != null) {
            section.getSubSections().forEach(subSection -> collectUnlabelled(subSection, unlabelled));
        }
        if (section.getFields() != null) {
            section.getFields().forEach(field -> collectUnlabelled(field, unlabelled));
        }
    }

    private void collectUnlabelled(UiField field, List<String> unlabelled) {
        if (field.getLabel() == null || field.getLabel().getText() == null) {
            unlabelled.add(field.getName());
        }
        if (field.getSubFields() != null) {
            field.getSubFields().forEach(subField -> collectUnlabelled(subField, unlabelled));
        }
    }

    private Map<String, IndexField> indexFields(String type) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/openaire/models/m-oa-" + type + ".json")) {
            Model model = mapper.readValue(in, Model.class);
            ResourceType resourceType = typeMapper.map(model, new ResourceType());
            assertEquals(type, resourceType.getName());
            return resourceType.getIndexFields().stream()
                    .collect(Collectors.toMap(IndexField::getName, f -> f, (a, b) -> a));
        }
    }

    private static void assertPath(Map<String, IndexField> fields, String name, String path) {
        assertTrue(fields.containsKey(name), "missing index field " + name + " in " + fields.keySet());
        assertEquals(path, fields.get(name).getPath());
    }
}
