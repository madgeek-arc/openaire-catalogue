package eu.openaire.catalogue.extension;

import gr.uoa.di.madgik.catalogue.domain.Model;
import gr.uoa.di.madgik.catalogue.domain.Section;
import gr.uoa.di.madgik.catalogue.domain.UiField;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every field of the lot1 domain class of a resource type, nested fields included, has a field in its OpenAIRE model,
 * so that records in lot1 form pass validation.
 */
class OpenAIREModelLot1CoverageTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"service", "provider", "datasource"})
    void modelCoversEveryLot1Field(String type) throws IOException {
        Set<String> modelFields = modelFields(type);
        List<String> missing = new ArrayList<>();
        for (JsonNode path : lot1Fields().get(type)) {
            if (!modelFields.contains(path.asString())) {
                missing.add(path.asString());
            }
        }
        assertEquals(List.of(), missing);
    }

    private JsonNode lot1Fields() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/lot1/lot1-fields.json")) {
            return mapper.readTree(in).get("fields");
        }
    }

    private Set<String> modelFields(String type) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/openaire/models/m-oa-" + type + ".json")) {
            Set<String> paths = new HashSet<>();
            for (Section section : mapper.readValue(in, Model.class).getSections()) {
                collect(section, paths);
            }
            return paths;
        }
    }

    private void collect(Section section, Set<String> paths) {
        if (section.getSubSections() != null) {
            section.getSubSections().forEach(subSection -> collect(subSection, paths));
        }
        if (section.getFields() != null) {
            section.getFields().forEach(field -> collect(field, "", paths));
        }
    }

    private void collect(UiField field, String prefix, Set<String> paths) {
        String path = prefix + field.getName();
        paths.add(path);
        if (field.getSubFields() != null) {
            field.getSubFields().forEach(subField -> collect(subField, path + ".", paths));
        }
    }
}
